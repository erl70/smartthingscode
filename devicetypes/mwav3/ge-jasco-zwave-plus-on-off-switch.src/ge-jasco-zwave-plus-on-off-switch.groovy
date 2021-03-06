/**
 *  GE/Jasco Z-Wave Plus On/Off Switch
 * 
 *  Contains code from https://github.com/nuttytree/Nutty-SmartThings/blob/master/devicetypes/nuttytree/ge-jasco-zwave-plus-on-off-switch.src/ge-jasco-zwave-plus-on-off-switch.groovy
 *
 *  Copyright 2020 Chris Nussbaum, Tim Grimley
 *  Contributors - Bradlee_S
 *  Thanks Chris for the original copy of this great code!
 *  Thanks Bradlee for the button programming to get this working in the new app's automations section
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
 *	Author: Tim Grimley
 *	Date: 08/31/2020
 *
 *	Changelog:
 *
 *  0.13 (12/02/2020) - Changed button values to support automations view in new app (see mapping note below)
 *  0.12 (09/23/2020) - Support added for double tap and hold, added code to make sure button values updated 
 *  0.11 (08/31/2020) - Initial Release Updated to Work with New Smartthings App
 *  
 *
 *   Button Mappings  NOTE - THIS IS A BREAKING CHANGE from prior versions and uses a single button.  
 *                    ALL prior automations will need to be re-programmed or updated when updating this DTH from old versions:
 *
 *   ACTION             BUTTON#    BUTTON ACTION
 *   Double-Tap Up        1        up_2x
 *   Double-Tap Down      1        down_2x  
 *   Double-Tap Up Hold   1        up_3x
 *   Double-Tap Down Hold 1        down_3x
 *   Double-Tap Release   1        down_4x
 * 
 *   Note - For double tap hold, tap twice and keep pressed up or down after second tap
 *   If options do not change, go to preferences and toggle "force settings update/refresh"
 */

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {
	definition (name: "GE Jasco Z-Wave Plus On Off Switch", namespace: "mwav3", author: "Tim Grimley") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Indicator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

		attribute "inverted", "enum", ["inverted", "not inverted"]
        
        command "doubleUp"
        command "doubleDown"
        command "inverted"
        command "notInverted"
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
		fingerprint mfr:"0063", prod:"4952", model: "3036", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3036", ver: "5.22", deviceJoinName: "GE Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3037", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3038", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3130", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3131", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3132", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
	}
    
    preferences {
        
        input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When On", "off": "When Off", "never": "Never"], defaultValue: "off"
        input "invertSwitch", "bool", title: "Invert Switch", description: "Invert switch? ", required: false
        input "forceupdate", "bool", title: "Force Settings Update/Refresh?", description: "Toggle to force settings update", required: false
        
        input (
            type: "paragraph",
            element: "paragraph",
            title: "Configure Association Groups:",
            description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
                         "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
                         "Devices are entered as a comma delimited list of IDs in hexadecimal format."
        )

        input (
            name: "requestedGroup2",
            title: "Association Group 2 Members (Max of 5):",
            type: "text",
            required: false
        )

        input (
            name: "requestedGroup3",
            title: "Association Group 3 Members (Max of 4):",
            type: "text",
            required: false
        )
    }

	tiles(scale:2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png", backgroundColor: "#00a0dc", nextState:"turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png", backgroundColor: "#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:"Turning On", action:"switch.off", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:"Turning Off", action:"switch.on", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}
        
        standardTile("doubleUp", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▲▲", backgroundColor: "#ffffff", action: "doubleUp", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png"
		}     
 
        standardTile("doubleDown", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▼▼", backgroundColor: "#ffffff", action: "doubleDown", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png"
		} 

		standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
			state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
			state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		}
        
		standardTile("inverted", "device.inverted", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "not inverted", label: "Not Inverted", action:"inverted", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchNotInverted.png", backgroundColor: "#ffffff"
			state "inverted", label: "Inverted", action:"notInverted", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchInverted.png", backgroundColor: "#ffffff"
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
        details(["switch", "doubleUp", "doubleDown", "indicator", "inverted", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
    log.debug "description: $description"
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    
    if (!device.currentValue("supportedButtonValues")) {
        sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x","up_3x","down_3x","down_4x"]), displayed:false)
    }
    
    result    
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	log.debug("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.debug("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    log.debug "---Double Tap--- ${device.displayName} sent ${cmd}"
	if (cmd.value == 255) {
    	createEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "Double-tap up (up_2x) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "Double-tap down (down_2x) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd) {
	 log.debug "---Double Tap and Hold--- ${device.displayName} sent ${cmd}"
	if (cmd.startLevel == 0) {
    	createEvent(name: "button", value: "up_3x", data: [buttonNumber: 1], descriptionText: "Double-tap up and hold (up_3x) on $device.displayName", isStateChange: true, type: "physical")    
        }
	else if (cmd.startLevel == 255) {
    	createEvent(name: "button", value: "down_3x", data: [buttonNumber: 1], descriptionText: "Double-tap down and hold (down_3x) on $device.displayName", isStateChange: true, type: "physical")
    }
 }
 
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
	log.debug "---Double Tap and Release--- ${device.displayName} sent ${cmd}"
	createEvent(name: "button", value: "down_4x", data: [buttonNumber: 1], descriptionText: "Double-tap Release (down_4x) on $device.displayName", isStateChange: true, type: "physical")    
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	createEvent(name: "numberOfButtons", value: 1, displayed: false)
            sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x","up_3x","down_3x","down_4x"]), displayed:false)
        }
        else {
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        	createEvent(name: "numberOfButtons", value: 0, displayed: false)
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
	def name = ""
    def value = ""
    def reportValue = cmd.configurationValue[0]
    switch (cmd.parameterNumber) {
        case 3:
            name = "indicatorStatus"
            value = reportValue == 1 ? "when on" : reportValue == 2 ? "never" : "when off"
            break
        case 4:
            name = "inverted"
            value = reportValue == 1 ? "true" : "false"
            break
        default:
            break
    }
	createEvent([name: name, value: value, displayed: false])
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.debug "---MANUFACTURER SPECIFIC REPORT V2---"
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
    state.manufacturer=cmd.manufacturerName
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)	
    createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	log.debug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

// handle commands
def configure() {
    def cmds = []
    // Get current config parameter values
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
    
    delayBetween(cmds,500)
}

def updated() {
    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def nodes = []
    def cmds = []

	if (settings.requestedGroup2 != state.currentGroup2) {
        nodes = parseAssocGroupList(settings.requestedGroup2, 2)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
        state.currentGroup2 = settings.requestedGroup2
    }

    if (settings.requestedGroup3 != state.currentGroup3) {
        nodes = parseAssocGroupList(settings.requestedGroup3, 3)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
        state.currentGroup3 = settings.requestedGroup3
    }
    
    switch (ledIndicator) {
		case "on":
			indicatorWhenOn()
			break
		case "off":
			indicatorWhenOff()
			break
		case "never":
			indicatorNever()
			break
		default:
			indicatorWhenOff()
			break
	}
    
    switch (invertSwitch) {
    	case "false":
        	notInverted()
            break
        case "true":
        	inverted()
            break
        default:
        	notInverted()
	}      
	
	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
    
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x","up_3x","down_3x","down_4x"]), displayed:false) 
    
    log.debug "---Preferences Updated--- ${device.displayName} sent ${cmds}"
}

void indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

void indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

void indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

void inverted() {
	sendEvent(name: "inverted", value: "inverted", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()))
}

void notInverted() {
	sendEvent(name: "inverted", value: "not inverted", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()))
}

def doubleUp() {
	sendEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "Double-tap up (up_2x) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "Double-tap down (down_2x) on $device.displayName", isStateChange: true, type: "digital")
}

def poll() {
	def cmds = []
    cmds << zwave.switchBinaryV1.switchBinaryGet().format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
}

def refresh() {
	def cmds = []
	cmds << zwave.switchBinaryV1.switchBinaryGet().format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
     
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x","up_3x","down_3x","down_4x"]), displayed:false) 
      
}


// Private Methods

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
                }
                else if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    log.warn "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                log.warn "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
    
    return nodes
}
