definition(
	name: "Link Switch and Lock",
	namespace: "aderusha",
	author: "aderusha",
	description: "Link state between a switch and a lock. Switch On <=> Locked, Switch Off <=> Unlocked",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-locks@2x.png"
)

preferences {
	section("When this switch is turned on") {
		input "switch1", "capability.switch", multiple: false, required: true
	}
	section("Lock this lock") {
		input "lock1", "capability.lock", multiple: false, required: true
	}
}    

def installed()
{   
	subscribe(switch1, "switch.on", onHandler)
	subscribe(switch1, "switch.off", offHandler)
	subscribe(lock1, "lock.locked", lockedHandler)
	subscribe(lock1, "lock.unlocked", unlockedHandler)
}

def updated()
{
	unsubscribe()
	subscribe(switch1, "switch.on", onHandler)
	subscribe(switch1, "switch.off", offHandler)
	subscribe(lock1, "lock.locked", lockedHandler)
	subscribe(lock1, "lock.unlocked", unlockedHandler)
}

def onHandler(evt) {
	log.debug evt.value
	log.debug "Locking lock: $lock1"
	lock1.lock()
}

def offHandler(evt) {
	log.debug evt.value
	log.debug "Unlocking lock: $lock1"
	lock1.unlock()
}

def lockedHandler(evt) {
	log.debug evt.value
	log.debug "Turning on switch: $switch1"
   	switch1.on()
}

def unlockedHandler(evt) {
	log.debug evt.value
	log.debug "Turning off switch: $switch1"
   	switch1.off()
}