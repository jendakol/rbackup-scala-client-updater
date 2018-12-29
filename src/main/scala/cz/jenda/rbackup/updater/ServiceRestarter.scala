package cz.jenda.rbackup.updater

import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.SystemUtils

import scala.language.postfixOps
import scala.sys.process._

sealed trait ServiceRestarter {
  def stop(): Unit
  def start(): Unit
}

object ServiceRestarter {
  def getOrElse(failure: => Nothing): ServiceRestarter = {
    if (SystemUtils.IS_OS_WINDOWS) {
      new WindowsServiceRestarter
    } else if (SystemUtils.IS_OS_LINUX) {
      new LinuxServiceRestarter
    } else {
      failure
    }
  }
}

class WindowsServiceRestarter extends ServiceRestarter with StrictLogging {
  override def stop(): Unit = {
    "net stop rbackup-client" !;
    "cmd /C del RUNNING_PID > NUL" !;
    logger.info("Service stopped")
  }

  override def start(): Unit = {
    "net start rbackup-client" !!;
    logger.info("Service started")
  }
}

class LinuxServiceRestarter extends ServiceRestarter with StrictLogging {
  override def stop(): Unit = ???
  override def start(): Unit = ???
}
