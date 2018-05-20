/**
 *  Smart Motion-Light Link
 *
 *  Copyright 2016 William Billett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smart Motion-Light Link",
    namespace: "wjbillett",
    author: "William Billett",
    description: "Allows setting lighting level based on time of day when mothion is detected.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on this light") {
        input "myswitch", "capability.switchLevel", title: "myswitch", required: true, multiple: false

    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(themotion, "motion.active", motionDetectedHandler)

}


// TODO: implement event handlers

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    if("active" == evt.value && "on" != myswitch.currentSwitch) {
    myswitch.setLevel(1) // also turns on the switch
  } else if ("inactive" == evt.value && "off" != myswitch.currentSwitch) {
    myswitch.setLevel(90)
  }

}

