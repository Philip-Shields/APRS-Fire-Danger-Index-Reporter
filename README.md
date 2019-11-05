# APRS-Fire-Danger-Index-Reporter
A Java APRS fire danger reporter that gets its information from the Australian BOM JSON stream
This stand alone java application reads the JSON stream from the BOM for a particular town. It 
calculates the possible fire danger in real time and puts an icon on the aprs.fi map.
You have to supply your call sign, your aprs pass code, coordinates for the icon on the aprs.fi map 
and the url to your town eg:
http://www.bom.gov.au/fwo/IDN60801/IDN60801.95896.json (for the town of Albury in this example).
The application expects Java 8. I used Eclipse to run it and/or turn it into an executable jar file.
It has three .txt files thet are:
teleNumber.txt a file that holds a current beacon number. Beacons have their own number which increments *1.
lastRainAlbury.txt a file that holds the last time it rained. I'll change it to a more generic name like 
lastRainRecorded.txt
wxnow.txt a file that Cumulus normally generates, I don't use Cumulus any more. This application generates it 
and it is read by UI-VIEW32 and transmitted as a weather beacon on 2 meters.
Complile and run the application. Someone may write a gui for it someday. 
The application is basically a fun experiment and its outputs including the fire danger rating are experimental and 
are not intended to be relied on for human, animal or property safety. 
Your official fire department notices are for that.
cheers and 73,
Phil 
