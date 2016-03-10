/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.common

import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.zip.GZIPInputStream

/**
 * @author T. Huang
 * @version $Id:$
 */
class TarGzipUtil {

   private static Log logger = LogFactory.getLog(TarGzipUtil.class)

   /** Untar an input file into an output file.

    * The output file is created in the output folder, having the same name
    * as the input file, minus the '.tar' extension.
    *
    * @param inputFile the input .tar file
    * @param outputDir the output directory file.
    * @throws IOException
    * @throws FileNotFoundException
    *
    * @return The {@link List} of {@link File}s with the untared content.
    * @throws ArchiveException
    */
   public static List<File> unTar(final File inputFile, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {

      logger.debug "Untaring ${inputFile.absolutePath} to dir ${outputDir.absolutePath}"

      List<File> untaredFiles = []
      InputStream is = new FileInputStream(inputFile)
      TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is)
      TarArchiveEntry entry
      while ((entry = (TarArchiveEntry) debInputStream.nextEntry) != null) {
         final File outputFile = new File(outputDir, entry.name)
         if (entry.directory) {
            logger.debug "Attempting to write output directory ${outputFile.absolutePath}"
            if (!outputFile.exists()) {
               logger.debug "Attempting to create output directory ${outputFile.absolutePath}"
               if (!outputFile.mkdirs()) {
                  throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()))
               }
            }
         } else {
            logger.debug "Creating output file ${outputFile.absolutePath}"
            final OutputStream outputFileStream = new FileOutputStream(outputFile)
            IOUtils.copy(debInputStream, outputFileStream)
            outputFileStream.close()
         }
         untaredFiles.add(outputFile)
      }
      debInputStream.close()

      return untaredFiles
   }

   /**
    * Ungzip an input file into an output file.
    * <p>
    * The output file is created in the output folder, having the same name
    * as the input file, minus the '.gz' extension.
    *
    * @param inputFile the input .gz file
    * @param outputDir the output directory file.
    * @throws IOException
    * @throws FileNotFoundException
    *
    * @return The {@File} with the ungzipped content.
    */
   public static File unGzip(final File inputFile, final File outputDir) throws FileNotFoundException, IOException {

      logger.debug "Ungzipping ${inputFile.absolutePath} to dir ${outputDir.absolutePath}"

      File outputFile = new File(outputDir, inputFile.name.substring(0, inputFile.name.length() - 3))

      GZIPInputStream gis = new GZIPInputStream(new FileInputStream(inputFile))
      FileOutputStream out = new FileOutputStream(outputFile)

      IOUtils.copy(gis, out);

      out.close()

      return outputFile;
   }

}
