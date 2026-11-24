
const int TRIG1 = 5;
const int ECHO1 = 18;

const int TRIG2 = 19;
const int ECHO2 = 4;

const int TRIG3 = 25;
const int ECHO3 = 26;


const int LED1 = 27;
const int LED2 = 14;
const int LED3 = 12;


float measureDistance(int trigPin, int echoPin) {

  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  unsigned long duration = pulseIn(echoPin, HIGH, 30000);

  if (duration == 0) {
    return -1;
  }

  float distance = duration * 0.0343 / 2.0;
  return distance;
}

void setup() {

  Serial.begin(115200);


  pinMode(TRIG1, OUTPUT);
  pinMode(ECHO1, INPUT);

  pinMode(TRIG2, OUTPUT);
  pinMode(ECHO2, INPUT);

  pinMode(TRIG3, OUTPUT);
  pinMode(ECHO3, INPUT);

  // LED 핀
  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(LED3, OUTPUT);

  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);
  digitalWrite(LED3, LOW);

  Serial.println("===== 초음파 센서 3개 시작 =====");
}

void loop() {

  // 순서대로 측정
  float distance1 = measureDistance(TRIG1, ECHO1);
  delay(50);

  float distance2 = measureDistance(TRIG2, ECHO2);
  delay(50);

  float distance3 = measureDistance(TRIG3, ECHO3);



  if (distance1 > 0 && distance1 <= 50)
    digitalWrite(LED1, HIGH);
  else
    digitalWrite(LED1, LOW);

  if (distance2 > 0 && distance2 <= 50)
    digitalWrite(LED2, HIGH);
  else
    digitalWrite(LED2, LOW);

  if (distance3 > 0 && distance3 <= 50)
    digitalWrite(LED3, HIGH);
  else
    digitalWrite(LED3, LOW);

  // =========================
  // 시리얼 출력
  // =========================

  Serial.print("센서1 : ");
  if (distance1 < 0)
    Serial.print("측정 실패");
  else {
    Serial.print(distance1, 1);
    Serial.print(" cm");
  }

  Serial.print(" | 센서2 : ");
  if (distance2 < 0)
    Serial.print("측정 실패");
  else {
    Serial.print(distance2, 1);
    Serial.print(" cm");
  }

  Serial.print(" | 센서3 : ");
  if (distance3 < 0)
    Serial.print("측정 실패");
  else {
    Serial.print(distance3, 1);
    Serial.print(" cm");
  }

  Serial.println();

  delay(100);
}