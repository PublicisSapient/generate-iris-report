How to use:
- Install IRIS according to instructions here: https://github.com/electronicarts/IRIS, and see convo here: https://github.com/electronicarts/IRIS/issues/8
- Ensure you have Java (8 min) and Maven (min 3.9.9)
- cd into accessible-pdf-generator-cl folder
- run mvn install
- run mvn exec:java -Dexec.args="useGui=false|true videoPath=/full/path/to/video.[mov, mp4, etc.] irisPath=/full/path/to/iris/executable/file pdfName=fileName.pdf"
- All args are optional, but you will be prompted to enter all values
- File will save at accessible-pdf-generator-cl/fileName.pdf