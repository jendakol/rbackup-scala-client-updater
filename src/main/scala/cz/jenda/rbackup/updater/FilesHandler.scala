package cz.jenda.rbackup.updater
import better.files.File
import com.typesafe.scalalogging.StrictLogging

object FilesHandler extends StrictLogging {
  private val files = Seq(
    "conf",
    "lib",
    "public",
    "updater.jar"
  )

  def handle(dirWithUpdate: File): Unit = {
    files.foreach { fileName =>
      val oldFile = File(fileName + ".old")
      val newFile = dirWithUpdate / fileName
      val file = File(fileName)

      logger.debug(s"Deleting old backup file ($fileName)")
      oldFile.delete(swallowIOExceptions = true)

      if (newFile.exists) {
        logger.debug(s"Creating new backup file ($fileName)")
        file moveTo oldFile
        logger.debug(s"Copying updated file ($fileName)")
        newFile moveTo file

        logger.info(s"$fileName replaced")
      } else {
        logger.info(s"Won't update $fileName, it's not present in the update")
      }
    }

    dirWithUpdate.delete(true)
  }

  def recover(): Unit = {
    files.foreach { fileName =>
      val oldFile = File(fileName + ".old")
      val failedFile = File(fileName + ".failed")
      val file = File(fileName)

      logger.debug(s"Deleting old failed file ($fileName)")
      failedFile.delete(swallowIOExceptions = true)

      if (oldFile.exists) {
        logger.debug(s"Creating new failed ($fileName)")
        file moveTo failedFile
        logger.debug(s"Recovering old file ($fileName)")
        oldFile moveTo file

        logger.info(s"$fileName replaced")
      } else {
        logger.info(s"Won't recover $fileName, old version is not present")
      }
    }
  }
}
