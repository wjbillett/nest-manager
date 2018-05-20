/**
 *  Mode Lighting
 *
 *  Copyright 2015 Bruce Ravenel
 *
 *	09-17-2015 0.9
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
    name: "Mode Lighting",
    namespace: "bravenel",
    author: "Bruce Ravenel",
    description: "Set Dimmer Levels Based on Modes, with Motion on/off/disabled, button on, and Master on/off",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
    page(name: "selectDimmers")
    page(name: "motionSettings")
    page(name: "otherSettings")
	
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectDimmers() {
	dynamicPage(name: "selectDimmers", title: "Dimmers, modes and levels", nextPage: "motionSettings", uninstall: true) {

		section("When this master dimmer") { 
			input "master", "capability.switchLevel", multiple: false, title: "Is turned on", required: true, submitOnChange: true
		}

		section("These slave dimmers") {
			input "slaves", "capability.switchLevel", multiple: true, title: "Will also be turned on", required: false
		}

		section("With dimmer levels") {
       	input "modesX", "mode", multiple: true, title: "for these modes", required: true, defaultValue: ["Day", "Evening", "Night"], submitOnChange: true
//        	input "modesX", "mode", multiple: true, title: "For these modes", required: true, submitOnChange: true
        
			if(modesX) {
				def defaults = [90, 30, 10]
//				def defaults = [0, 0, 0]
				def n = 0
				modesX.each {
					setModeLevel(it, "level$it", defaults[n])
					n = n + 1        	
				}
			}
		}
	}
}

def setModeLevel(thisMode, modeVar, defaults) {
	def result = input modeVar, "number", title: "Level for $thisMode", required: true, defaultValue: defaults > 0 ? defaults : null
    return result
}

def motionSettings() {
	dynamicPage(name:"motionSettings",title: "Motion sensor(s)", nextPage: "otherSettings", uninstall: false) {
    
		section("Turn them on when there is motion") {
			input "motions", "capability.motionSensor", title: "On these motion sensors", required: false, multiple: true
			input "disable", "capability.switch", title: "Switch to disable motion", required: false, multiple: false
		}
        
		section("Turn them off") {
			input "turnOff", "boolean", title: "When there is no motion", required: false, defaultValue: false
			input "minutes", "number", title: "For this many minutes", required: false, multiple: false
		}
	
		section(title: "Motion options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
   		}    
	}
}

def otherSettings() {
	dynamicPage(name:"otherSettings", uninstall: false, install: true) {

		section("Turn master and slaves on with this") {
			input "button", "capability.momentary", title: "button", multiple: false, required: false
		}

		section("These extra switches will be turned off") {
			input "offSwitches", "capability.switch", title: "when the master is turned off", multiple: true, required: false
		}
        
		section {
			label title: "Assign a name:", required: false
		}
   	}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}


def initialize() {
	subscribe(master, "switch.on", switchOnHandler)
	subscribe(master, "switch.off", switchOffHandler)
	subscribe(master, "level", levelHandler)
	slaves.each {subscribe(it, "level", levelHandler)}
	subscribe(location, modeChangeHandler)
	subscribe(motions, "motion.active", motionOnHandler)
	subscribe(disable, "switch", disableHandler)
	if(turnOff) {subscribe(motions, "motion.inactive", motionOffHandler)}
	subscribe(button, "switch.on", switchOnHandler)
    
	state.modeLevels = [:]
	for(m in modesX) {
		def level = settings.find {it.key == "level$m"}
		state.modeLevels << [(m):level.value]
	}

	state.currentMode = location.mode in modesX ? location.mode : modes[0]
	state.dimLevel = state.modeLevels[state.currentMode]
	state.motionOffDismissed = true
	state.motionDisabled = (disable) ? disable.currentSwitch == "on" : false
	state.masterOff = master.currentSwitch == "off"
}

def switchesOn() {
	state.motionOffDismissed = true    		// use this variable instead of unschedule() to kill pending off()
	state.masterOff = false
	master.setLevel(state.dimLevel)
	slaves.each {it.setLevel(state.dimLevel)}
}

def switchOnHandler(evt) {
	if(state.masterOff) switchesOn() 
}

def motionOnHandler(evt) {
	if(state.motionDisabled) return
	state.motionOffDismissed = true
	if(allOk && state.masterOff) switchesOn() 
}

def disableHandler(evt) {
	state.motionDisabled = evt.value == "on"
}

def levelHandler(evt) {      				// allows a dimmer to change the current dimLevel
	if(evt.value == state.dimLevel) return
	state.dimLevel = evt.value
	switchesOn()
}

def switchOffHandler(evt) {
	state.dimLevel = state.modeLevels[state.currentMode]
	state.masterOff = true
	slaves?.off()
	offSwitches?.off()
}

def switchesOff() {                    		
	if(state.motionOffDismissed || state.motionDisabled) return  
	if(allOk) {
		state.dimLevel = state.modeLevels[state.currentMode]
		state.masterOff = true
		master.off()
		slaves?.off()
		offSwitches?.off()
	}
}

def motionOffHandler(evt) {  				// called when motion goes inactive, check all sensors
	if(state.motionDisabled) return
	if(allOk) {
		def noMotion = true
		motions.each {noMotion = noMotion && it.currentMotion == "inactive"}
		state.motionOffDismissed = noMotion
		if(noMotion) {if(minutes) runIn(minutes*60, switchesOff) else switchesOff()}
	}
}

def modeChangeHandler(evt) {
	if(state.currentMode == location.mode || !(location.mode in modesX)) return   // no change or not one of our modes
	state.currentMode = location.mode			   
	state.dimLevel = state.modeLevels[state.currentMode]
// the next two lines brighten any lights on when new mode is brighter than previous
	if(master.currentSwitch == "on" && master.currentLevel < state.dimLevel) master.setLevel(state.dimLevel)
	slaves.each {if(it.currentSwitch == "on" && it.currentLevel < state.dimLevel) it.setLevel(state.dimLevel)}
}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
//	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
//	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
//  ST had a bug with respect to time zones.  may be fixed now        
//		def start = timeToday(starting).time
		def start = timeToday(starting,location.timeZone).time
//		def stop = timeToday(ending).time
		def stop = timeToday(ending,location.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
//	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}