#include <Wire.h>
#include <MPU6050.h>
#include <math.h>

// =========================================================
// 핀 설정 (Pin Configuration)
// =========================================================
// 센서 1 & LED 1 (빨간불)
const int TRIG1 = 19;
const int ECHO1 = 4;
const int LED1 = 27;

// 센서 2 & LED 2 (노란불)
const int TRIG2 = 5;
const int ECHO2 = 18;
const int LED2 = 33; // 노란불

// MPU6050 I2C 핀
const int SDA_PIN = 21;
const int SCL_PIN = 22;

// 장애물 감지 기준 거리 (cm)
const float DANGER_DISTANCE = 50.0;

MPU6050 mpu;

// =========================================================
// 초음파 거리 측정 함수
// =========================================================
float measureDistance(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // 30ms 타임아웃 (30000us)
  unsigned long duration = pulseIn(echoPin, HIGH, 30000);

  if (duration == 0) {
    return -1;
  }

  float distance = duration * 0.0343 / 2.0;
  return distance;
}

void setup() {
  Serial.begin(115200);

  // 초음파 센서 핀 설정
  pinMode(TRIG1, OUTPUT);
  pinMode(ECHO1, INPUT);

  pinMode(TRIG2, OUTPUT);
  pinMode(ECHO2, INPUT);

  // LED 핀 설정 및 초기화
  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);

  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);

  // MPU6050 기울기 센서 초기화
  Wire.begin(SDA_PIN, SCL_PIN);
  mpu.initialize();

  Serial.println("===== 초음파 2개 + MPU6050 + LED 통합 시스템 시작 =====");
}

void loop() {
  // 1. 센서 1 측정
  float distance1 = measureDistance(TRIG1, ECHO1);
  delay(30); // 센서 간 혼선 방지 딜레이

  // 2. 센서 2 측정
  float distance2 = measureDistance(TRIG2, ECHO2);

  // 3. MPU6050 기울기(Pitch) 측정
  int16_t ax, ay, az, gx, gy, gz;
  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
  float pitch = atan2((float)ax, sqrt((float)ay * ay + (float)az * az)) * 180.0 / PI;

  // =========================
  // LED 1:1 독립 제어
  // =========================
  if (distance1 > 0 && distance1 <= DANGER_DISTANCE) {
    digitalWrite(LED1, HIGH);
  } else {
    digitalWrite(LED1, LOW);
  }

  if (distance2 > 0 && distance2 <= DANGER_DISTANCE) {
    digitalWrite(LED2, HIGH);
  } else {
    digitalWrite(LED2, LOW);
  }

  // =========================
  // 시리얼 모니터 출력
  // =========================
  Serial.print("센서1 : ");
  if (distance1 < 0) {
    Serial.print("측정 실패");
  } else {
    Serial.print(distance1, 1);
    Serial.print(" cm");
    if (distance1 <= DANGER_DISTANCE) {
      Serial.print(" (장애물 감지!)");
    }
  }

  Serial.print(" | 센서2 : ");
  if (distance2 < 0) {
    Serial.print("측정 실패");
  } else {
    Serial.print(distance2, 1);
    Serial.print(" cm");
    if (distance2 <= DANGER_DISTANCE) {
      Serial.print(" (장애물 감지!)");
    }
  }

  Serial.print(" | 기울기 : ");
  Serial.print(pitch, 1);
  Serial.print(" 도");

  Serial.println();

  delay(100);
}