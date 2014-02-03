package com.reactor.accio.pipeline

import com.reactor.base.patterns.pull.FlowControlActor
import com.reactor.base.patterns.pull.FlowControlArgs

class AccioPipeline(args:FlowControlArgs) extends FlowControlActor(args) {

  def receive = {
    case _ =>
  }
}