import cv2
import numpy as np
import win32com.client
import pythoncom
import serial
import threading
import time
import os
import re
import io
import uuid
import requests
import json
from datetime import datetime, timezone

# ==========================================
# 1. 포트 및 기본 설정
# ==========================================
PORT_BOARD1 = 'COM5'
PORT_BOARD2 = 'COM6'
BAUD_RATE = 115200

SERVER_IP = "16.179.249.107"
MEASUREMENT_URL = f"http://{SERVER_IP}:8081/api/measurements"
IMAGE_UPLOAD_URL = f"http://{SERVER_IP}:8081/api/images/upload"
DEVICE_ID = "DG-ESP32-001"
LATEST_TRIP_URL = f"http://{SERVER_IP}:8081/api/trips/latest"
RULE_VERSION = "2026.10.15-v2"

USER_ID = 1
PITCH_BASELINE = -80.0

TRIP_ID = None
TRIP_VIDEO_URL = ""
TRIP_STATUS_URL = ""

TEST_LATITUDE = 37.5559
TEST_LONGITUDE = 126.9723

board1_raw = ""
board2_raw = ""
board1_data = "B1 (Front+Gyro): Waiting..."
board2_data = "B2 (Rear): Waiting..."
running = True

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
save_dir = os.path.join(BASE_DIR, "captures")
if not os.path.exists(save_dir):
    os.makedirs(save_dir)

log_path = os.path.join(BASE_DIR, "hazard_log.txt")
try:
    with open(log_path, "a", encoding="utf-8") as f:
        f.write(f"\n=== 시스템 시작: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} ===\n")
    print(f"[OK] 로그 파일 준비 완료: {log_path}")
except Exception as e:
    print(f"[ERROR] 로그 파일 생성 오류: {e}")


# ==========================================
# 2. 백엔드 통합 전송 (/api/measurements)
# ==========================================
def _map_risk_level(danger_level):
    mapping = {
        "CRITICAL": "DANGER", "DANGER": "DANGER",
        "WARNING": "WARNING", "CAUTION": "WARNING",
        "SAFE": "SAFE", "CLEAR": "SAFE",
    }
    return mapping.get(danger_level, "SAFE")


def upload_image(frame):
    try:
        _, img_encoded = cv2.imencode('.jpg', frame)
        files = {'file': ('capture.jpg', io.BytesIO(img_encoded.tobytes()), 'image/jpeg')}
        resp = requests.post(IMAGE_UPLOAD_URL, files=files, timeout=3)
        if resp.status_code in [200, 201]:
            return resp.json().get('imageUrl')
        print(f"[이미지 업로드 실패] HTTP {resp.status_code} | {resp.text[:200]}")
    except Exception as e:
        print(f"[이미지 업로드 실패] {e}")
    return None


def parse_sensor_readings(text):
    """시리얼 텍스트에서 (센서번호, 거리mm) 리스트 추출"""
    if not text:
        return []
    clean = re.sub(r'기울기\s*:[^|]*', '', text)
    clean = re.sub(r'Gyro\s*:[^|]*', '', clean, flags=re.IGNORECASE)
    matches = re.findall(r'(\d+)\s*:\s*(\d+\.?\d*)\s*cm', clean, re.IGNORECASE)
    readings = []
    for s_num, dist_cm in matches:
        mm = int(float(dist_cm) * 10)
        if mm > 20:
            readings.append((int(s_num), mm))
    return readings


def send_measurement(level, danger_level, reason, trip_id,
                     front_readings, rear_readings,
                     front_min_mm, rear_min_mm,
                     pitch_raw, pitch_diff,
                     board1_ok, board2_ok,
                     detected_object=None, confidence=None,
                     max_box_ratio=None, std_dev=None, image_url=None):
    """0.2초 주기 1회분을 /api/measurements로 통합 전송"""
    def _send():
        now_utc = datetime.now(timezone.utc)
        ts = now_utc.strftime('%Y-%m-%dT%H:%M:%S.') + f"{now_utc.microsecond // 1000:03d}Z"
        risk_level = _map_risk_level(danger_level)

        matched = []
        if front_min_mm and front_min_mm < 800:
            matched.append("FRONT_US_CLOSE")
        if rear_min_mm and rear_min_mm < 300:
            matched.append("REAR_US_CLOSE")
        if pitch_diff and pitch_diff >= 6.0:
            matched.append("TILT_SLOPE")
        if detected_object:
            matched.append("CAMERA_OBJECT")
        if max_box_ratio and max_box_ratio >= 0.08:
            matched.append("CAMERA_BOX_LARGE")

        if pitch_diff and pitch_diff >= 20.0:
            slope_status = "DANGER"
        elif pitch_diff and pitch_diff >= 6.0:
            slope_status = "WARNING"
        else:
            slope_status = "NORMAL"

        all_readings = []
        for sid, mm in front_readings:
            if mm > 20:
                all_readings.append({"sensorId": sid, "distanceMm": mm})
        for sid, mm in rear_readings:
            if mm > 20:
                all_readings.append({"sensorId": sid + 2, "distanceMm": mm})

        has_object = detected_object is not None
        

        payload = {
            "measurementId": str(uuid.uuid4()),
            "tripId":        trip_id,
            "deviceId":      DEVICE_ID,
            "detectedAt":    ts,
            "latitude":      TEST_LATITUDE,
            "longitude":     TEST_LONGITUDE,
            "riskStage":     level,
            "riskLevel":     risk_level,
            "matchedRules":  matched,
            "ruleVersion":   RULE_VERSION,
            "reason":        reason,
            "board1Valid":   board1_ok,
            "board2Valid":   board2_ok,
            "ultrasonic": {
                "frontValid": board1_ok,
                "rearValid":  board2_ok,
                "frontMinMm": front_min_mm if front_min_mm and front_min_mm < 9990 else None,
                "rearMinMm":  rear_min_mm if rear_min_mm and rear_min_mm < 9990 else None,
                "readings":   all_readings
            },
            "tilt": {
                "valid":       board1_ok and pitch_diff is not None,
                "pitchRaw":    round(pitch_raw, 2) if pitch_raw else None,
                "pitchDiff":   round(pitch_diff, 2) if pitch_diff else None,
                "slopeStatus": slope_status
            },
            "camera": {
                "valid":          True,
                "objectDetected": has_object,
                "detectedObject": detected_object,
                "confidence":     round(confidence, 3) if confidence else None,
                "maxBoxRatio":    round(max_box_ratio, 4) if max_box_ratio and max_box_ratio > 0 else None,
                "stdDev":         round(float(std_dev), 2) if std_dev is not None else None,
                "imageUrl":       image_url
            }
        }

        try:
            headers = {'Content-Type': 'application/json'}
            resp = requests.post(MEASUREMENT_URL, data=json.dumps(payload), headers=headers, timeout=1)
            if resp.status_code not in [200, 201]:
                print(f"[백엔드 에러] HTTP {resp.status_code} | {resp.text[:200]}")
        except Exception as e:
            print(f"[백엔드 통신 실패] {e}")

    threading.Thread(target=_send, daemon=True).start()


# ==========================================
# 3. 유틸리티 및 파싱 함수
# ==========================================
def draw_text_with_bg(img, text, pt, font=cv2.FONT_HERSHEY_SIMPLEX, scale=0.5, color=(0, 255, 255), thickness=1, bg_color=(0, 0, 0), alpha=0.6):
    (text_w, text_h), baseline = cv2.getTextSize(text, font, scale, thickness)
    x, y = pt
    overlay = img.copy()
    cv2.rectangle(overlay, (x - 4, y - text_h - 4), (x + text_w + 4, y + baseline + 4), bg_color, -1)
    cv2.addWeighted(overlay, alpha, img, 1 - alpha, 0, img)
    cv2.putText(img, text, (x, y), font, scale, color, thickness, cv2.LINE_AA)

def preprocess_for_shadow(img):
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    h, s, v = cv2.split(hsv)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    v_equalized = clahe.apply(v)
    hsv_corrected = cv2.merge([h, s, v_equalized])
    return cv2.cvtColor(hsv_corrected, cv2.COLOR_HSV2BGR)

def clean_text(text):
    text = text.replace("보드", "Board").replace("초음파", "US").replace("기울기", "Gyro")
    text = text.replace("위험", "DANGER").replace("경보", "WARN").replace("정상", "OK")
    text = re.sub(r'[^\x00-\x7F]+', '', text).strip()
    return text if text else "Data Stream..."

def parse_min_distance(text, is_front=False):
    if not text:
        return 999.0
    text_clean = re.sub(r'기울기\s*:[^|]*', '', text)
    text_clean = re.sub(r'Gyro\s*:[^|]*', '', text_clean, flags=re.IGNORECASE)
    text_clean = re.sub(r'\b\d+\s*:', '', text_clean)
    matches = re.findall(r'(\d+\.?\d*)\s*cm', text_clean, re.IGNORECASE)
    distances = []
    for m in matches:
        try:
            val = float(m)
            if val > 2.0:
                distances.append(val)
        except ValueError:
            pass
    return min(distances) if distances else 999.0

def parse_gyro_angles(text):
    if not text:
        return 0.0, 0.0
    p_match = re.search(r'기울기\s*[:=]\s*(-?\d+\.?\d*)', text)
    if p_match is None:
        p_match = re.search(r'\bPitch\s*[:=]\s*(-?\d+\.?\d*)', text, re.IGNORECASE)
    if p_match:
        try:
            raw = float(p_match.group(1))
            diff = abs(raw - PITCH_BASELINE)
            return raw, diff
        except ValueError:
            pass
    return 0.0, 0.0


# ==========================================
# 4. 시리얼 통신 스레드
# ==========================================
def read_board1():
    global board1_raw, board1_data, running
    while running:
        try:
            ser1 = serial.Serial(PORT_BOARD1, BAUD_RATE, timeout=0.1)
            print(f"[연결 성공] 전방 ESP32 보드 1 ({PORT_BOARD1})")
            while running and ser1.is_open:
                if ser1.in_waiting > 0:
                    raw = ser1.readline().decode('utf-8', errors='ignore').strip()
                    if raw and not raw.startswith("---"):
                        board1_raw = raw
                        board1_data = clean_text(raw)
                    if ser1.in_waiting > 100:
                        ser1.reset_input_buffer()
                time.sleep(0.005)
        except Exception:
            board1_raw = ""
            board1_data = "B1 Offline"
            time.sleep(1)

def read_board2():
    global board2_raw, board2_data, running
    while running:
        try:
            ser2 = serial.Serial(PORT_BOARD2, BAUD_RATE, timeout=0.1)
            print(f"[연결 성공] 후방 ESP32 보드 2 ({PORT_BOARD2})")
            while running and ser2.is_open:
                if ser2.in_waiting > 0:
                    raw = ser2.readline().decode('utf-8', errors='ignore').strip()
                    if raw and not raw.startswith("---"):
                        board2_raw = raw
                        board2_data = clean_text(raw)
                    if ser2.in_waiting > 100:
                        ser2.reset_input_buffer()
                time.sleep(0.005)
        except Exception:
            board2_raw = ""
            board2_data = "B2 Offline"
            time.sleep(1)

threading.Thread(target=read_board1, daemon=True).start()
threading.Thread(target=read_board2, daemon=True).start()


# ==========================================
# 5. 음성 안내 (TTS)
# ==========================================
is_speaking = False

def speak_warning(text):
    global is_speaking
    if not is_speaking and text:
        is_speaking = True
        def run_tts():
            global is_speaking
            try:
                pythoncom.CoInitialize()
                speaker = win32com.client.Dispatch("SAPI.SpVoice")
                speaker.Speak(text)
            except Exception as e:
                print("[TTS 에러]:", e)
            finally:
                is_speaking = False
                pythoncom.CoUninitialize()
        threading.Thread(target=run_tts, daemon=True).start()


# ==========================================
# 6. AI 모델 로드 및 카메라 초기화
# ==========================================
yolo_weights = os.path.join(BASE_DIR, "yolov4-tiny.weights")
yolo_cfg = os.path.join(BASE_DIR, "yolov4-tiny.cfg")
coco_names = os.path.join(BASE_DIR, "coco.names")
depth_model = os.path.join(BASE_DIR, "model-small.onnx")

net_yolo = cv2.dnn.readNet(yolo_weights, yolo_cfg)
layer_names = net_yolo.getLayerNames()
output_layers = [layer_names[i - 1] for i in net_yolo.getUnconnectedOutLayers()]

classes = []
if os.path.exists(coco_names):
    with open(coco_names, "r") as f:
        classes = [line.strip() for line in f.readlines()]
else:
    classes = ["person", "chair", "obstacle"]

net_depth = cv2.dnn.readNet(depth_model)

CAMERA_INDEX = 1
cap = cv2.VideoCapture(CAMERA_INDEX, cv2.CAP_DSHOW)


if not cap.isOpened():
    print(f"[ERROR] 외장 웹캠(장치 번호: {CAMERA_INDEX})을 열 수 없습니다. 프로그램을 종료합니다.")
    exit(1)

cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)


# ==========================================
# 7. 최신 tripId 조회 및 상태 체크 함수
# ==========================================
def get_latest_trip_id():
    try:
        resp = requests.get(LATEST_TRIP_URL, params={"userId": USER_ID}, timeout=3)
        if resp.status_code == 200:
            return resp.json().get('data', {}).get('tripId')
    except Exception:
        pass
    return None

def check_trip_started(trip_id):
    """백엔드에 현재 tripId의 상태가 여전히 STARTED인지 확인"""
    try:
        status_url = f"http://{SERVER_IP}:8081/api/trips/{trip_id}/status"
        resp = requests.get(status_url, timeout=1)
        if resp.status_code == 200:
            status = resp.json().get('data', {}).get('tripStatus')
            # 만약 STARTED가 아니면(예: COMPLETED 또는 STOPPED) False 반환
            return status == 'STARTED'
    except Exception:
        pass

    return True


print("==================================================")
print("[시스템 대기 중] 앱에서 주행을 시작해주세요...")
print("==================================================")

active_trip_id = None
while running:
    active_trip_id = get_latest_trip_id()
    if active_trip_id:
        # 추가 검증: 가져온 최신 tripId가 실제로 STARTED 상태인지 확인 후 진입
        if check_trip_started(active_trip_id):
            print(f"\n[주행 ID 확인 완료] tripId={active_trip_id}")
            break
        else:
            print(f"[대기 중] 이미 종료된 tripId({active_trip_id})입니다. 새로운 주행을 시작해주세요...")
    time.sleep(1.5)

TRIP_ID = active_trip_id
TRIP_VIDEO_URL = f"http://{SERVER_IP}:8081/api/trips/{TRIP_ID}/video"
print(f"[센서 및 블랙박스 녹화를 시작합니다. tripId={TRIP_ID}]")


# ==========================================
# 블랙박스 동영상 저장
# ==========================================
RECORD_FPS = 8.0
fourcc = cv2.VideoWriter_fourcc(*'XVID')
blackbox_filename = f"blackbox_{datetime.now().strftime('%Y%m%d_%H%M%S')}.avi"
blackbox_path = os.path.join(save_dir, blackbox_filename)
out_video = cv2.VideoWriter(blackbox_path, fourcc, RECORD_FPS, (640 * 2, 480))
if out_video.isOpened():
    print(f"[블랙박스 녹화 시작] 저장 경로: {blackbox_path}")
else:
    print("[블랙박스 녹화 실패] VideoWriter를 열 수 없습니다")

last_record_time = 0
last_warning_time = 0
last_log_time = 0
last_status_check_time = 0  # 앱 주행 종료 상태 체크용 타이머


# ==========================================
# 8. 메인 처리 루프
# ==========================================
try:
    while running:
        curr_time = time.time()

        # --- [주행 상태 주기적 체크: 앱에서 주행 종료 시 자동 종료] ---
        if curr_time - last_status_check_time >= 3.0:
            if not check_trip_started(TRIP_ID):
                print(f"\n[주행 종료 감지] 앱에서 주행이 종료되었습니다 (tripId={TRIP_ID}). 프로그램을 안전하게 종료합니다.")
                break
            last_status_check_time = curr_time

        ret, frame = cap.read()
        if not ret or frame is None:
            time.sleep(0.01)
            continue

        h, w, _ = frame.shape
        frame_area = h * w

        # --- [A] Depth Map 분석 ---
        shadow_reduced_frame = preprocess_for_shadow(frame)
        blob_depth = cv2.dnn.blobFromImage(shadow_reduced_frame, 1/255.0, (256, 256), (123.675, 116.28, 103.53), swapRB=True, crop=False)
        net_depth.setInput(blob_depth)
        depth = net_depth.forward()[0, 0]
        depth = cv2.resize(depth, (w, h))

        depth_norm = cv2.normalize(depth, None, 0, 255, norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_8U)
        depth_colormap = cv2.applyColorMap(depth_norm, cv2.COLORMAP_INFERNO)

        roi_top, roi_bottom = int(h * 0.5), int(h * 0.95)
        roi_left, roi_right = int(w * 0.2), int(w * 0.8)

        depth_roi = depth_norm[roi_top:roi_bottom, roi_left:roi_right]
        std_dev = np.std(depth_roi)
        is_road_danger = std_dev > 50.0

        # --- [B] YOLOv4 객체 탐지 ---
        yolo_frame = frame.copy()
        blob_yolo = cv2.dnn.blobFromImage(yolo_frame, 1/255.0, (320, 320), (0, 0, 0), swapRB=True, crop=False)
        net_yolo.setInput(blob_yolo)
        outs = net_yolo.forward(output_layers)

        class_ids, confidences, boxes = [], [], []
        max_box_ratio = 0.0

        for out in outs:
            for detection in out:
                scores = detection[5:]
                class_id = np.argmax(scores)
                confidence = scores[class_id]
                if confidence > 0.25:
                    center_x, center_y = int(detection[0] * w), int(detection[1] * h)
                    bw, bh = int(detection[2] * w), int(detection[3] * h)
                    x, y = int(center_x - bw / 2), int(center_y - bh / 2)
                    boxes.append([x, y, bw, bh])
                    confidences.append(float(confidence))
                    class_ids.append(class_id)

        indexes = cv2.dnn.NMSBoxes(boxes, confidences, 0.25, 0.4)
        has_yolo_object = len(indexes) > 0

        if has_yolo_object:
            for i in indexes.flatten():
                x, y, bw, bh = boxes[i]
                box_ratio = (bw * bh) / frame_area
                if box_ratio > max_box_ratio:
                    max_box_ratio = box_ratio
                label = str(classes[class_ids[i]]) if class_ids[i] < len(classes) else "Object"
                cv2.rectangle(yolo_frame, (x, y), (x + bw, y + bh), (0, 255, 0), 2)
                draw_text_with_bg(yolo_frame, f"{label} {confidences[i]:.2f}", (x, max(y - 10, 20)), scale=0.5, color=(0, 255, 0))

        # --- [C] 센서 독립 파싱 ---
        front_readings = parse_sensor_readings(board1_raw)
        rear_readings = parse_sensor_readings(board2_raw)
        front_us_dist = parse_min_distance(board1_raw, is_front=False)
        rear_us_dist = parse_min_distance(board2_raw, is_front=False)
        pitch_raw, pitch_diff = parse_gyro_angles(board1_raw)

        front_min_mm = int(front_us_dist * 10) if front_us_dist < 999 else None
        rear_min_mm = int(rear_us_dist * 10) if rear_us_dist < 999 else None
        board1_ok = board1_data != "B1 Offline" and board1_raw != ""
        board2_ok = board2_data != "B2 Offline" and board2_raw != ""

        # =========================================================================
        # --- [D] 위험 5단계 제어 로직 (초음파 단독 우선 반영) ---
        # =========================================================================
        level = 1
        danger_level = "CLEAR"
        reason = "SAFE"
        tts_msg = ""
        box_color = (0, 0, 255)
# 5단계: 전방 초음파 15cm 이내 또는 큰 물체/심한 경사
        if front_us_dist <= 15.0 or max_box_ratio >= 0.30 or pitch_diff >= 30.0:
            level = 5
            danger_level = "CRITICAL"
            reason = f"CRITICAL HAZARD (FrontUS:{front_us_dist:.1f}cm, PitchDiff:{pitch_diff:.1f})"
            tts_msg = "비상! 전방 충돌 및 전복 위험, 즉시 정지하세요."
            box_color = (0, 0, 255)

        # 4단계: 전방 초음파 30cm 이내 또는 중형 물체/경사
        elif front_us_dist <= 30.0 or max_box_ratio >= 0.15 or pitch_diff >= 20.0:
            level = 4
            danger_level = "DANGER"
            reason = f"FRONT HAZARD (FrontUS:{front_us_dist:.1f}cm, PitchDiff:{pitch_diff:.1f})"
            tts_msg = "위험! 전방 근접 또는 경사 주의."
            box_color = (0, 0, 255)

        # 3단계: 전방 초음파 60cm 이내 또는 접근 중
        elif front_us_dist <= 60.0 or pitch_diff >= 12.0 or (is_road_danger and has_yolo_object):
            level = 3
            danger_level = "WARNING"
            reason = f"APPROACHING (FrontUS:{front_us_dist:.1f}cm, PitchDiff:{pitch_diff:.1f})"
            tts_msg = "경고! 장애물 또는 경사로 접근 중."
            box_color = (0, 165, 255)

        # 2단계: 확실한 후방 장애물 감지 시
        elif rear_us_dist > 0 and rear_us_dist <= 50.0:
            level = 2
            danger_level = "CAUTION"
            reason = f"REAR CAUTION ({rear_us_dist:.1f}cm)"
            tts_msg = "주의! 후방 장애물 접근."
            box_color = (0, 255, 255)
            
        # 2단계: 경사로 감지 시
        elif pitch_diff >= 10.0:
            level = 2
            danger_level = "CAUTION"
            reason = f"SLIGHT SLOPE (PitchDiff:{pitch_diff:.1f})"
            tts_msg = "주의! 경사로 감지."
            box_color = (0, 255, 255)
            
        # 2단계: 카메라와 뎁스가 동시에 위험을 포착했을 때만
        elif is_road_danger and has_yolo_object:
            level = 2
            danger_level = "CAUTION"
            reason = "OBJECT & ROAD HAZARD"
            tts_msg = "주의! 전방 장애물 및 도로 위험."
            box_color = (0, 255, 255)

        # 1단계: 아무것도 해당하지 않을 때 안전 상태
        else:
            level = 1
            danger_level = "SAFE"
            reason = "SAFE"
            tts_msg = ""
            box_color = (0, 255, 0)
            
        # --- [E] 화면 Overlay 텍스트 생성 ---
        draw_text_with_bg(yolo_frame, f"ESP-1 (Front US+Gyro): {board1_data} (PitchDiff:{pitch_diff:.1f})", (15, h - 50), scale=0.42, color=(0, 255, 255), bg_color=(20, 20, 20))
        draw_text_with_bg(yolo_frame, f"ESP-2 (Rear US): {board2_data} (Parsed:{rear_us_dist:.1f}cm)", (15, h - 25), scale=0.42, color=(0, 255, 255), bg_color=(20, 20, 20))
        draw_text_with_bg(yolo_frame, f"[LEVEL {level} - {danger_level}] {reason}", (20, 50), scale=0.75, color=box_color, bg_color=(0, 0, 0), thickness=2)

        cv2.rectangle(depth_colormap, (roi_left, roi_top), (roi_right, roi_bottom), box_color, 2)

        combined_view = np.hstack((yolo_frame, depth_colormap))
        draw_text_with_bg(combined_view, "YOLOv4 Detection", (20, 25), scale=0.55, color=(255, 255, 255))
        draw_text_with_bg(combined_view, f"MiDaS Depth Map (StdDev: {std_dev:.1f})", (w + 20, 25), scale=0.55, color=(255, 255, 255))

        # --- 블랙박스 녹화 ---
        if out_video.isOpened() and curr_time - last_record_time >= 1.0 / RECORD_FPS:
            out_video.write(combined_view)
            last_record_time = curr_time

# --- [F] 0.2초 주기: 로그 + /api/measurements 통합 전송 ---
        if curr_time - last_log_time >= 0.2:
            try:
                now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                log_entry = f"[{now_str}] [LV{level}-{danger_level}] Front_US:{front_us_dist:.1f}cm | Rear_US:{rear_us_dist:.1f}cm | PitchDiff:{pitch_diff:.1f} | Reason:{reason}\n"
                with open(log_path, "a", encoding="utf-8") as f:
                    f.write(log_entry)
                    f.flush()
            except Exception as e:
                print(f"[로그 쓰기 에러]: {e}")

            cam_object = None
            cam_confidence = None
            cam_image_url = None
            if has_yolo_object and indexes is not None and len(indexes) > 0:
                best_i = indexes.flatten()[0]
                cam_object = str(classes[class_ids[best_i]]) if class_ids[best_i] < len(classes) else "Object"
                cam_confidence = confidences[best_i]
                if level in (4, 5):  # 필요시 조건 조정
                    cam_image_url = upload_image(yolo_frame)

            send_measurement(
                level=level,
                danger_level=danger_level,
                reason=reason,
                trip_id=TRIP_ID,
                front_readings=front_readings,
                rear_readings=rear_readings,
                front_min_mm=front_min_mm,
                rear_min_mm=rear_min_mm,
                pitch_raw=pitch_raw,
                pitch_diff=pitch_diff,
                board1_ok=board1_ok,
                board2_ok=board2_ok,
                detected_object=cam_object,
                confidence=cam_confidence,
                max_box_ratio=max_box_ratio if max_box_ratio > 0 else None,
                std_dev=std_dev,
                image_url=cam_image_url,
            )

            last_log_time = curr_time

        # --- [G] 경보 및 UI 스냅샷 캡쳐 저장 ---
        if level in [4, 5]:
            if curr_time - last_warning_time > 1.5:
                speak_warning(tts_msg)
                now_str = datetime.now().strftime("%Y%m%d_%H%M%S")
                img_filename = os.path.join(save_dir, f"level{level}_{now_str}.jpg")
                cv2.imwrite(img_filename, combined_view)
                last_warning_time = curr_time
        elif level in [2, 3]:
            if curr_time - last_warning_time > 3.0:
                speak_warning(tts_msg)
                last_warning_time = curr_time

        # --- [H] 화면 출력 ---
        cv2.imshow("Wheelchair Navigation Assist System", combined_view)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

finally:
    running = False

    if 'out_video' in globals() and out_video.isOpened():
        out_video.release()
        print(f"[블랙박스 저장 완료] 파일 경로: {blackbox_path}")

        try:
            print("[백엔드 전송 중] 블랙박스 영상 업로드...")
            with open(blackbox_path, 'rb') as f:
                files = {'file': (blackbox_filename, f, 'video/x-msvideo')}
                response = requests.post(IMAGE_UPLOAD_URL, files=files, timeout=(5, 300))

            if response.status_code == 200:
                video_url = response.json().get('imageUrl')
                print(f"[백엔드 전송 성공] 영상 URL: {video_url}")
                try:
                    patch_resp = requests.patch(TRIP_VIDEO_URL, json={"videoUrl": video_url}, timeout=5)
                    if patch_resp.status_code == 200:
                        print(f"[주행 연결 완료] tripId={TRIP_ID}")
                    else:
                        print(f"[주행 연결 실패] HTTP {patch_resp.status_code}")
                except Exception as e:
                    print(f"[주행 연결 에러]: {e}")
            else:
                print(f"[백엔드 전송 실패] HTTP {response.status_code}")
        except Exception as e:
            print(f"[백엔드 통신 에러]: {e}")

    if cap is not None and cap.isOpened():
        cap.release()
    cv2.destroyAllWindows()
    print("시스템이 안전하게 종료되었습니다.")