package com.tzulitai

import java.io.{InputStreamReader, BufferedReader}
import java.util.concurrent.atomic.AtomicInteger

import akka.util.Timeout
import scala.concurrent.duration._
import scala.sys.Prop
import akka.pattern.ask

/**
 * Hello world!
 *
 */

object TransformationApp {
  def main(args: Array[String]): Unit = {
    TransformationFrontend.main(Seq("2551").toArray)
    NewBackendJvm.spawn(TransformationBackend.getClass, redirectStream = false)
    NewBackendJvm.spawn(TransformationBackend.getClass, redirectStream = false)
    NewBackendJvm.spawn(TransformationBackend.getClass, redirectStream = false)
  }
}

object NewBackendJvm {
  def spawn(clazz: Class[_], redirectStream: Boolean): Unit = {
    val className = clazz.getCanonicalName.dropRight(1) // drop the $ prefix
    val sep = System.getProperty("file.separator")
    val classpath = System.getProperty("java.class.path")
    val path = System.getProperty("java.home") + sep + "bin" + sep + "java"

    println("className: " + className)

    val processBuilder = new ProcessBuilder(path, "-cp", classpath, className)
    val procCmd = processBuilder.command().toString

    println("processBuilder: " + procCmd)

    processBuilder.redirectErrorStream(redirectStream)
    val process = processBuilder.start()
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))

    println(reader.readLine())
    reader.close()
  }
}