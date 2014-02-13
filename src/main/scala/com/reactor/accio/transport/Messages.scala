package com.reactor.accio.transport

import com.reactor.accio.metadata.MetaData
import scala.collection.mutable.ArrayBuffer
import com.reactor.accio.metadata.Candidate
import com.reactor.accio.metadata.confluence.ConfluenceNode

case class MetadataContainer(metadata:MetaData)
case class CandidateList(candidates:ArrayBuffer[Candidate])
case class ConfluenceNodeList(confluenceNodes:ArrayBuffer[Any])
case class IdList(ids:ArrayBuffer[String])
