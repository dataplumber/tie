package gov.nasa.gibs.tie.handlers.common

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Simple threaded job pool implementation
 */
class JobPool {
   static Log log = LogFactory.getLog(JobPool.class)

   static final Set jobs = new HashSet()
   int max
   def pool

   def JobPool(int max) {
      this.max = max
      pool = Executors.newFixedThreadPool(max)
   }

   def work = { job, yield ->
      log.trace("Job ${job.name} start.")
      yield.call()
      log.trace("Job ${job.name} cleanup.")
      synchronized (jobs) {
         log.trace(jobs.toString())
         def jobRef = jobs.find {
            it.name == job.name
         }
         if (!jobRef) {
            log.error("Job ${job.name} not found in service queue.")
         } else {
            if (!jobs.remove(jobRef)) {
               log.trace("Failed to remove job ${job.name}.")
            }
            log.trace("Job ${job.name} has been removed from service queue.")
         }
         if (log.traceEnabled) {
            log.trace("Current ${jobs.size()} active job(s) -")
            jobs.each {
               log.trace("${it.name}:${it.received}")
            }
         }
      }
      log.trace("Job ${job.name} done.")
   }

   def execute(String name, Closure yield) {
      synchronized (jobs) {
         //if (jobs.size() < max) {
            def job = [name: name, received: new Date()]
            jobs.add(job)
            pool.submit({ work(job, yield) } as Callable)
            log.trace("Job ${name} is now in progress.")
            //return true
         //}
      }
      return true
   }

   synchronized Map getJob(String name) {
      Map result = jobs.find {
         it.name == name
      }
      if (result)
         return result.asImmutable()
      return result
   }

   synchronized List getJobs() {
      return jobs.asList()
   }

   synchronized int getJobCount() {
      return jobs.size()
   }

   synchronized def shutdown() {
      pool.shutdown()
   }
}