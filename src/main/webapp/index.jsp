<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
    <head>
        <title>Upload Progress</title>
        <style>
            #container {
                width: fit-content;
            }

            #progressContainer {
                width: 100%;
                height: 20px;
                background-color: #f3f3f3;
                border: 1px solid #ddd;
                border-radius: 10px;
                overflow: hidden;
                margin: 20px 0;
            }

            #progressBar {
                width: 0;
                height: 100%;
                background-color: #4caf50;
                color: white;
                text-align: center;
                line-height: 20px;
                border-radius: 10px 0 0 10px;
                transition: width 0.3s ease-in-out;
            }
        </style>
    </head>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.1.1/crypto-js.min.js"></script>
    <body>
    <div id="container">
        <h1>breakpoint-upload-java</h1>
        <input id="fileInput" type="file" name="file" required>
        <button onclick="uploadFile()">upload</button>
        <div id="progressContainer">
            <div id="progressBar">0%</div>
        </div>
    </div>

    <script>
        async function uploadFile() {
            const file = document.getElementById('fileInput').files[0]
            const chunkSize = 1024 * 1024 * 2
            const totalChunks = Math.ceil(file.size / chunkSize);
            const fileMD5 = getFileMD5(file)

            let chunksReceived = await getFileUploadInfo(fileMD5) ?? new Array(totalChunks)

            let uploadedCount = 0
            const progressBar = document.getElementById('progressBar')
            progressBar.style.width = "0"
            progressBar.textContent = "0"

            const onProgress = () => {
                uploadedCount++
                const progress = Math.round((uploadedCount / totalChunks) * 100)
                progressBar.style.width = progress + "%"
                progressBar.textContent = progress + "%"
            }

            const chunks = []
            for (let i = 0; i < totalChunks; i++) {
                if (!chunksReceived[i]) {
                    const start = i * chunkSize
                    const end = Math.min(start + chunkSize, file.size)
                    chunks.push(file.slice(start, end))
                } else {
                    uploadedCount++
                    chunks.push(null)
                }
            }

            const maxConcurrency = 5

            await uploadChunksWithConcurrency(chunks, file, fileMD5, totalChunks, maxConcurrency, onProgress)
        }

        async function uploadChunksWithConcurrency(chunks, file, fileMD5, totalChunks, maxConcurrency, callback) {
            const activeUploads = []

            for (let index = 0; index < chunks.length; index++) {
                const chunk = chunks[index]
                if (!chunk) continue;

                const uploadTask = uploadChunk(chunk, index, file, fileMD5, totalChunks, callback);

                activeUploads.push(uploadTask);

                if (activeUploads.length >= maxConcurrency) {
                    await Promise.race(activeUploads);
                }

                activeUploads.splice(activeUploads.findIndex(p => p.isFulfilled), 1);
            }

            await Promise.all(activeUploads);
        }

        async function uploadChunk(chunk, index, file, fileMD5, totalChunks, callback) {
            const formData = new FormData();
            formData.append('file', chunk);
            formData.append('chunkIndex', index);
            formData.append('totalChunks', totalChunks);
            formData.append('fileName', file.name);
            formData.append('fileMD5', fileMD5);

            try {
                const response = await fetch("upload", {
                    method: "POST",
                    body: formData,
                })

                if (!response.ok) {
                    throw new Error(`Chunk ${index} upload failed with status ${response.status}`);
                }

                callback();
            } catch (error) {
                console.error(`Error uploading chunk ${index}:`, error);
                throw error;
            }
        }

        function getFileMD5(file) {
            return CryptoJS.MD5(file).toString()
        }

        async function getFileUploadInfo(fileMD5) {
            const formData = new URLSearchParams();
            formData.append('fileMD5', fileMD5);

            try {
                const response = await fetch('upload/status', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: formData.toString(),
                });

                if (!response.ok) {
                    throw new Error(`Failed to fetch upload info: ${response.status}`);
                }

                return await response.json();
            } catch (error) {
                console.error('Error fetching upload info:', error);
                return null;
            }
        }
    </script>
    </body>
</html>