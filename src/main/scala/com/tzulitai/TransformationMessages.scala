package com.tzulitai

/**
 * Created by tzulitai on 4/16/15.
 */

// The message classes between actors
case class TransformationJob(text: String)
case class TransformationResult(text: String)
case class JobFailed(reason: String, job: TransformationJob)
case object BackendRegistration
