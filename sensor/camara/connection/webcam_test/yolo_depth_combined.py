import cv2
import numpy as np

# 1. YOLOv4-Tiny 모델 로드
net_yolo = cv2.dnn.readNet("yolov4-tiny.weights", "yolov4-tiny.cfg")
layer_names = net_yolo.getLayerNames()
output_layers = [layer_names[i - 1] for i in net_yolo.getUnconnectedOutLayers()]

# COCO 클래스 이름 로드
classes = []
try:
    with open("coco.names", "r") as f:
        classes = [line.strip() for line in f.readlines()]
except:
    classes = ["person", "chair", "obstacle"] # 파일이 없을 경우 기본 클래스

# 2. MiDaS Depth 모델 로드
net_depth = cv2.dnn.readNet("model-small.onnx")

# 3. 외장 웹캠 연결
cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
if not cap.isOpened():
    cap = cv2.VideoCapture(1, cv2.CAP_DSHOW)

print("위험 노면 감지 포함 통합 시스템을 시작합니다. 'q'를 누르면 종료합니다.")

while True:
    ret, frame = cap.read()
    if not ret or frame is None:
        continue

    h, w, _ = frame.shape

    # --- [A] Depth Estimation & 노면 분석 ---
    blob_depth = cv2.dnn.blobFromImage(frame, 1/255.0, (256, 256), (123.675, 116.28, 103.53), swapRB=True, crop=False)
    net_depth.setInput(blob_depth)
    depth = net_depth.forward()[0, 0]
    depth = cv2.resize(depth, (w, h))
    depth_norm = cv2.normalize(depth, None, 0, 255, norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_8U)
    depth_colormap = cv2.applyColorMap(depth_norm, cv2.COLORMAP_INFERNO)

    # --- [B] 화면 하단 바닥 영역(ROI) 노면 위험 분석 ---
    # 화면 하단 40% 영역을 전방 노면으로 지정
    roi_top = int(h * 0.6)
    roi_bottom = h
    roi_left = int(w * 0.2)
    roi_right = int(w * 0.8)

    # ROI 추출
    depth_roi = depth_norm[roi_top:roi_bottom, roi_left:roi_right]
    
    # ROI 내 깊이 변화율(표준편차/경사도) 계산
    std_dev = np.std(depth_roi)
    is_danger = std_dev > 45.0  # 노면 불균형/턱 감지 임계값 (필요 시 조절)

    # --- [C] YOLOv4-Tiny 객체 탐지 ---
    yolo_frame = frame.copy()
    blob_yolo = cv2.dnn.blobFromImage(yolo_frame, 1/255.0, (416, 416), (0, 0, 0), swapRB=True, crop=False)
    net_yolo.setInput(blob_yolo)
    outs = net_yolo.forward(output_layers)

    class_ids, confidences, boxes = [], [], []
    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]
            if confidence > 0.3:
                center_x, center_y = int(detection[0] * w), int(detection[1] * h)
                bw, bh = int(detection[2] * w), int(detection[3] * h)
                x, y = int(center_x - bw / 2), int(center_y - bh / 2)
                boxes.append([x, y, bw, bh])
                confidences.append(float(confidence))
                class_ids.append(class_id)

    indexes = cv2.dnn.NMSBoxes(boxes, confidences, 0.3, 0.4)
    if len(indexes) > 0:
        for i in indexes.flatten():
            x, y, bw, bh = boxes[i]
            label = str(classes[class_ids[i]]) if class_ids[i] < len(classes) else "Object"
            confidence_val = confidences[i]
            cv2.rectangle(yolo_frame, (x, y), (x + bw, y + bh), (0, 255, 0), 2)
            cv2.putText(yolo_frame, f"{label} {confidence_val:.2f}", (x, max(y - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # --- [D] 시각화 및 위험 알림 표시 ---
    # Depth Map에 분석 중인 노면 ROI 영역 상자 그리기
    color = (0, 0, 255) if is_danger else (0, 255, 0)
    cv2.rectangle(depth_colormap, (roi_left, roi_top), (roi_right, roi_bottom), color, 2)

    if is_danger:
        # 위험 감지 시 경고 문구 출력
        cv2.putText(yolo_frame, "WARNING: UNEVEN ROAD / STEP!", (20, h - 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
        cv2.putText(depth_colormap, "DANGER DETECTED", (roi_left + 10, roi_top + 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)

    # 두 화면 가로 결합
    combined_view = np.hstack((yolo_frame, depth_colormap))
    
    cv2.putText(combined_view, "YOLOv4-Tiny Detection", (20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
    cv2.putText(combined_view, f"Depth Map (Road StdDev: {std_dev:.1f})", (w + 20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    cv2.imshow("Wheelchair Assist System - Hazard Detection", combined_view)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()