/*
 * Copyright (c) 2011, Dominic Cleal.
 * Released under the 2-clause BSD licence, see LICENCE.
 *
 * Reads switch state from pin 2, sends 0 or 1 (bytes) to serial connection
 * when the switch state changes to off or on, respectively.
 */

int state;

void setup() {
  Serial.begin(9600);
  pinMode(2, INPUT);
  state = digitalRead(2);
  pinMode(13, OUTPUT);
}

void loop() {
  int newstate = digitalRead(2);
  digitalWrite(13, newstate);
  if (newstate != state) {
    Serial.print(byte(newstate));
    state = newstate;
  }
}
