package cz.jenda.rbackup.updater

import better.files.File
import com.typesafe.scalalogging.StrictLogging
import io.sentry.Sentry
import io.sentry.event.EventBuilder
import org.apache.commons.lang3.SystemUtils
import org.scalatest.concurrent.Eventually
import org.scalatest.time._

import scala.util._
import scala.util.control.NonFatal

object Main extends App with StrictLogging with Eventually {
  final val SentryDsn: Option[String] = Some("https://d7de6a6c0bf344d69bfe3631d3c5dfb3@sentry.io/1361797")
  final val Version: String = null

  SentryDsn.foreach { dsn =>
    logger.debug("Configuring Sentry")
    val sentry = Sentry.init(dsn)
    sentry.setRelease(Version)
    sentry.addTag("app", "updater")
    sentry.setDist(if (SystemUtils.IS_OS_WINDOWS) "win" else "linux")
  }

  logger.info(s"Updater args: ${args.mkString("[", ", ", "]")}")

  if (args.length != 5) {
    dieWith("Required parameters: {old version} {new version} {env} {deviceId} {name of the dir with update}")
  }

  private val oldVersion = args(0)
  private val newVersion = args(1)
  private val env = args(2)
  private val deviceId = args(3)

  Sentry.getStoredClient.addTag("oldVersion", oldVersion)
  Sentry.getStoredClient.addTag("newVersion", newVersion)
  Sentry.getStoredClient.setEnvironment(env)
  Sentry.getStoredClient.setServerName(deviceId)

  logger.info(s"Updating from version $oldVersion to $newVersion")

  private val dirWithUpdate = File(args(4))
  logger.info("Dir with update: " + dirWithUpdate)

  if (!dirWithUpdate.exists) dieWith(s"The dir '$dirWithUpdate' doesn't exist")

  logger.debug(s"Platform: ${SystemUtils.OS_NAME} ${SystemUtils.OS_VERSION}")

  logger.debug(s"DeviceId: $deviceId}")

  private val serviceRestarter = ServiceRestarter.getOrElse(dieWith("OS not supported"))

  /* START */

  serviceRestarter.stop()

  FilesHandler.handle(dirWithUpdate)

  serviceRestarter.start()

  logger.info("Waiting for the service to become available...")

  try {
    waitForServiceRunning()
  } catch {
    case NonFatal(e) =>
      Sentry.capture {
        new EventBuilder()
          .withMessage("Update failed")
          .withExtra("oldVersion", oldVersion)
          .withExtra("newVersion", newVersion)
      }
      logger.warn("Service check failed, recovering", e)
      tryRecoverOldVersion()
  }

  logger.info("The service seems to be running, exiting")
  sys.exit(0)

  /* END */

  private def tryRecoverOldVersion(): Unit = {
    serviceRestarter.stop()
    FilesHandler.recover()
    serviceRestarter.start()

    try {
      waitForServiceRunning()
    } catch {
      case NonFatal(ex) =>
        Sentry.capture {
          new EventBuilder()
            .withMessage("Recovery failed")
            .withExtra("oldVersion", oldVersion)
            .withExtra("newVersion", newVersion)
        }

        dieWith("Unable to recover the service :-(", ex)
    }
  }

  private def waitForServiceRunning(): Unit = {
    eventually(timeout = timeout(Span(1, Minutes)), interval = interval(Span(1, Second))) {
      ServiceHealthCheck.getStatus.getOrThrow()
    }
  }

  private def dieWith(msg: String, cause: Throwable = null): Nothing = {
    logger.error(msg, cause)
    sys.exit(1)
  }

  private implicit class TryOps[A](val t: Try[A]) extends AnyVal {
    def getOrThrow(): A = t match {
      case Success(value) => value
      case Failure(e) => throw e
    }
  }
}
