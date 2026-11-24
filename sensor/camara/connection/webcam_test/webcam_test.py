import cv2
import numpy as np

# 1. COCO 데이터셋 클래스 이름 (사람, 자동차, 자전거 등 80개)
CLASSES = [
    "person", "bicycle", "car", "motorbike", "aeroplane", "bus", "train", "truck", "boat",
    "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
    "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
    "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
    "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
    "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
    "sofa", "pottedplant", "bed", "diningtable", "toilet", "tvmonitor", "laptop", "mouse",
    "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator",
    "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
]

# 2. YOLOv4-Tiny 모델 로드 (다운로드한 파일 이름과 일치해야 함)
net = cv2.dnn.readNet("yolov4-tiny.weights", "yolov4-tiny.cfg")

# OpenCV DNN 백엔드 설정 (CPU 사용)
net.setPreferableBackend(cv2.dnn.DNN_BACKEND_OPENCV)
net.setPreferableTarget(cv2.dnn.DNN_TARGET_CPU)

layer_names = net.getLayerNames()
output_layers = [layer_names[i - 1] for i in net.getUnconnectedOutLayers()]

# 3. 웹캠 연결
cap = cv2.VideoCapture(1)

if not cap.isOpened():
    print("웹캠을 열 수 없습니다.")
    exit()

print("AI 객체 탐지 시작! 'q'를 누르면 종료합니다.")

while True:
    ret, frame = cap.read()
    if not ret:
        print("영상을 받아오지 못했습니다.")
        break

    height, width, _ = frame.shape

    # 4. 이미지 전처리 (DNN 모델에 맞게 변환)
    blob = cv2.dnn.blobFromImage(frame, 1/255.0, (416, 416), (0, 0, 0), swapRB=True, crop=False)
    net.setInput(blob)
    outs = net.forward(output_layers)

    class_ids = []
    confidences = []
    boxes = []

    # 5. 인식 결과 분석
    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]

            # 확신도(confidence)가 30% 이상인 경우만 선별
            if confidence > 0.3:
                center_x = int(detection[0] * width)
                center_y = int(detection[1] * height)
                w = int(detection[2] * width)
                h = int(detection[3] * height)

                x = int(center_x - w / 2)
                y = int(center_y - h / 2)

                boxes.append([x, y, w, h])
                confidences.append(float(confidence))
                class_ids.append(class_id)

    # 6. 박스 중복 제거 (Non-Maximum Suppression)
    indexes = cv2.dnn.NMSBoxes(boxes, confidences, 0.3, 0.4)

    # 7. 화면에 박스 및 객체 정보(이름, 확률) 그리기
    if len(indexes) > 0:
        for i in indexes.flatten():
            x, y, w, h = boxes[i]
            label = str(CLASSES[class_ids[i]])
            confidence_val = confidences[i]

            color = (0, 255, 0) # 초록색 박스
            text = f"{label} {confidence_val:.2f}"

            # 바운딩 박스와 글자 출력
            cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
            cv2.putText(frame, text, (x, max(y - 10, 20)), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

    # 실시간 화면 출력
    cv2.imshow("Wheelchair Assist - Object Detection", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()