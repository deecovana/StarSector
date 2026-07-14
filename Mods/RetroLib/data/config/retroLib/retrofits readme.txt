retrofits.csv
- id: unique id
- tags: determine where retrofit is offered
- source: pre-retrofit ship hull id
- target: post-retrofit ship hull id
- cost: in credits OR leave blank for auto-generated value
- time: in days
- reputation:
	 4: COOPERATIVE
	 3: FRIENDLY
	 2: WELCOMING
	 1: FAVORABLE
	 0: NEUTRAL
	-1: SUSPICIOUS
	-2: INHOSPITABLE
	-3: HOSTILE
	-4: VENGEFUL
- commission: true or false

frameSourcesBlacklist.csv
- prevents the given hulls from being converted into frames

frameTargetsBlacklist.csv
- prevents frames from being converted into the given hulls