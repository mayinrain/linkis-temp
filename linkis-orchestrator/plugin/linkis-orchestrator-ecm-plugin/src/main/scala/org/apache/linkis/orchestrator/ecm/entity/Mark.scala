/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.linkis.orchestrator.ecm.entity

/**
  *
  *
  */
trait Mark {

  def getMarkId(): String


  def getMarkReq: MarkReq

}

class DefaultMark(markId: String, markReq: MarkReq) extends Mark {

  override def getMarkId(): String = markId

  override def getMarkReq: MarkReq = markReq

  override def hashCode(): Int = markId.hashCode

  override def equals(obj: Any): Boolean = obj match {
    case other: Mark => other.getMarkId().equals(markId)
    case _ => false
  }
}

class LoadBalanceMark(markId: String, firstMarkReq: MarkReq) extends DefaultMark(markId, firstMarkReq) {

}