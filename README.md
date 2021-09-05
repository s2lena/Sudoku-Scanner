**Youtube Demo:** https://youtu.be/HEHd0okd5HY

**Sudoku Scanner Application:**
- Using Camera Intent to capture the photo.
- Using OpenCV to extract the sudoku frame from the captured photo.
- Using TensorFlow Lite to deploy a CNN model to recognize the digit in each block of sudoku.
- Using a recursive algorithm to solve sudoku.

**CNN model:**
- Input size is [1, 28, 28, 1]
- Output size is [1, 10] with the probability of predict number in range [0, 9]

**My Android Studio version is Arctic Fox 2020.3.1**
**To Install OpenCV package:**
- Download the lastest version 3.4.15 of OpenCV.
- Copy the sdk folder into project directory
- Add this line in settings.gradle: include ':sdk'
