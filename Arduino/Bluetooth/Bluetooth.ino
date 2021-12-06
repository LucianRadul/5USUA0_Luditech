#include <SoftwareSerial.h>
SoftwareSerial mySerial(4,5); // RX, TX

int sensorPin = A0;
#define aref_voltage 5.0 
bool first = 0;
bool pressed = 0;
const int button = 2; //The pin for the button
unsigned long delayStart = 0; // the time the delay started

void button_press(){
  if(delayStart - millis()>=75){
    pressed = !pressed;
    mySerial.print(pressed?3:4); //Button pressed is sending a 3 to the bluetooth if it's released sending a 4.
    Serial.println(pressed?"Button pressed!":"Button released");
    delayStart = millis();
  }
  else{
//    Serial.println("bouncy, die bouncy");
  }
}

void setup() {
  // put your setup code here, to run once:
  mySerial.begin(115200);
  Serial.begin(9600);
  pinMode(LED_BUILTIN,OUTPUT);
  pinMode(button,INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(button), button_press, CHANGE);

  if(first){
    sendCommand("AT");
//    sendCommand("AT+RENEW");
//    sendCommand("AT+IMME");
//    sendCommand("AT+PIN000000");
    sendCommand("AT+ROLE0");
//    sendCommand("AT+UUIDFFE0");
//    sendCommand("AT+CHARFFE1");
//    sendCommand("AT+PASS");
    sendCommand("AT+NAMELuditech");
//    sendCommand("AT+START");
    }
  else{
    sendCommand("AT");
    sendCommand("AT+PIN");
    sendCommand("AT+NAME");
    sendCommand("AT+ROLE");
    sendCommand("AT+TYPE");
    sendCommand("AT+UUID");
    sendCommand("AT+CHAR");
    sendCommand("AT+HELP");
  }
}

void sendCommand(const char * command) {
  Serial.print("Command send :");
  Serial.println(command);
  mySerial.println(command);
  //wait some time
  delay(100);

  char reply[100];
  int i = 0;
  while (mySerial.available()) {
    reply[i] = mySerial.read();
    i += 1;
  }
  //end the string
  reply[i] = '\0';
  Serial.print(reply);
  Serial.println("Reply end");                 
  delay(50);
}

void writeSerialToBLE(int value) {
  mySerial.println(value);
}

void writeToBLE(const char *value) {
  Serial.print("Writing :");
  Serial.println(value);
  mySerial.write(value, strlen(value));
}


const int SWITCH_LED = 0x4;

const int lightsPin = LED_BUILTIN;

const initController() {
  pinMode(lightsPin, OUTPUT);
}

void executeCommand(int cmd[]) {
  switch(cmd[1]) {
    case SWITCH_LED: switchLight(cmd[3]); break;
  }
}

void switchLight(int v) {
  if(v == 0) {
    Serial.println("Switch the linght off");
  }else {
    Serial.println("Switch the linght on");
  }
  
  switchLED(v);
}

void switchLED(int value) {
    analogWrite(lightsPin, value);
}


const int START_COMMAND = 0x1;
int buff[10];
int currPos = 0;


void readCommand() {
  while (mySerial.available()) {
    int nc = mySerial.read();
    if (currPos > 0 || nc == START_COMMAND)
      parseNext(nc);
  }
}

void parseNext(int next) {
  buff[currPos++] = next;
  if (isCommandFullyRead()) {
    executeCommand(buff);
    currPos = 0;
  }
}

bool isCommandFullyRead() {
  return currPos > 2             && // command header is 3 bytes (START, Command type, num of parameters)
         buff[2] == currPos - 3;    // are all parameters already read
}

char j = 0;
void loop() {
  
  while (mySerial.available()) {
    j=mySerial.read();
  Serial.write(j);
  if(j=='1'){
    digitalWrite(LED_BUILTIN,HIGH);
  }
  else{
    digitalWrite(LED_BUILTIN,LOW);
  }
  }
  if(Serial.available()){
    mySerial.write(Serial.read());
  }
  
}             
