import cv2
import numpy as np

def hello():
    return "Hello, world!"


def capture_and_blur_video():
    cap = cv2.VideoCapture(0)

    while(True):
        # Capture frame-by-frame
        ret, frame = cap.read()

        # Apply a Gaussian blur filter to the frame
        blurred_frame = cv2.GaussianBlur(frame, (5, 5), 0)

        # Display the resulting frame
        cv2.imshow('frame', blurred_frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    # Release the capture
    cap.release()
    cv2.destroyAllWindows()

def process_frame(bitmapArray):
    # Convert the byte array to a NumPy array
    bitmap = np.frombuffer(bitmapArray, dtype=np.uint8)

    # Convert the 1D array to a 3D RGB array
    img = cv2.imdecode(bitmap, cv2.IMREAD_COLOR)

    # Convert the RGB image to grayscale
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # Apply a Gaussian blur filter to the grayscale image
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)

    print("blurred",blurred)