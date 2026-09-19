Just a random personal project I work on every now and then.

It's a pretty basic image codec that is very customizable. I don't expect this to compete with major formsts, but it is entertaining to work on it and watch it spectacularly fail sometimes.

## Features:

### 16 Byte header

- 4 magic bytes for identification 
- Width and Height stored as big-endian shorts
- 8 extra flags for various information for the decoder.

### Hue / Saturation Subsampling

Hue and saturation data is stored at a variable ratio to the value data. There is a quirk that if the two are not evenly divisible, there will be monochrome strips at the right and bottom edge. Fancy subsampling makes the encoder use the average of the subsampling block instead of the top left one.

### RLE

Standard run-length encoding using 0xFF as a flag for signalling a run. Any natural 0xFF value is decreased to a 0xFE before compression.

### Close Enough Encoding

Takes pixels with variably similar values and merges them into a RLE run using the first value. Fancy CEE takes the average of the run and uses that instead.

### The app itself

Powered by JavaFX, the app has a user-friendly design with very granular encoding settings and an option to export a bitmap file from a compressed .jif file.