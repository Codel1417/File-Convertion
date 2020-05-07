# Batch convert files using handbrake cli

This batch converts videos in a set folder and logs processed videos. If a video was previously converted it will be ignored. If the converted video was larger than the original, the original file is kept.

This program creates two threads to take advantage of the 2 nvenc encoders on the GTX 1070

## Notes

The program creates a file called RUN that, when removed, stoppes the threads when they finish a video.
