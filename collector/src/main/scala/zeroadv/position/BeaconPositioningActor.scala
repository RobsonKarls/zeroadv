package zeroadv.position

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.Logging
import zeroadv._
import zeroadv.PositionedAgents
import zeroadv.PositionedBeacon
import scala.Some
import zeroadv.BeaconsSpottings

class BeaconPositioningActor(
  receivedAdvParser: ReceivedAdvParser,
  beaconPosFromSpottings: BeaconPosFromSpottings,
  includeBeaconSpotting: BeaconSpotting => Boolean,
  positionedBeaconSink: PositionedBeacon => Any,
  spottingsRssisLimit: Int) extends Actor with Logging {

  private var agents = PositionedAgents(Nil)
  private var beacons = BeaconsSpottings(Map())

  def receive = {
    case adv: ReceivedAdv => {
      receivedAdvParser.parse(adv) match {
        case None => //logger.error("Cannot parse adv: " + adv)
        case Some(parsed) => {
          if (includeBeaconSpotting(parsed)) {
            logger.debug(s"Received adv: $adv")

            val (spottings, newBeacons) = beacons.addSpotting(parsed, spottingsRssisLimit)
            beacons = newBeacons

            val positionedBeacon = beaconPosFromSpottings.calculate(agents, spottings)
            positionedBeaconSink(positionedBeacon)
          }
        }
      }
    }

    case pa: PositionedAgents => agents = pa
  }
}
