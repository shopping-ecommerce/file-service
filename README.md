# File-Service - Quản Lý File & Upload S3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/shopping-ecommerce/file-service/actions) [![Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen.svg)](https://codecov.io/gh/shopping-ecommerce/file-service) [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot) [![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/) [![AWS S3](https://img.shields.io/badge/AWS%20S3-orange.svg)](https://aws.amazon.com/s3/)

## 📋 Mô Tả
File-Service là một microservice backend quản lý upload/download/xóa file, tích hợp **AWS S3** cho storage và **Rekognition** cho validation hình ảnh (moderation, confidence threshold 80%). Xây dựng bằng **Spring Boot 3.x**, sử dụng **MongoDB** để track metadata (file_mgmt collection: id, contentType, size, path, md5 checksum, ownerId), fallback local storage (D:/upload). Hỗ trợ multipart upload (max 10MB/file, 10MB/request), download (byte[] or Resource), delete by URLs, và validate-many images (returns validation results with labels/confidence).

Dự án tập trung vào bảo mật (PreAuthorize UPLOAD_FILE cho upload), performance (md5 checksum), và integration (Feign-ready, public download).

### 🏗️ Architecture
Kiến trúc microservices với File-Service làm core cho media handling. Các thành phần chính:
- **Communication**: REST API (multipart), S3 SDK (upload/delete).
- **Storage**: AWS S3 (primary), local fallback (D:/upload), MongoDB metadata.
- **Validation**: AWS Rekognition (analyze labels, confidence >80%).
- **Security**: JWT (OAuth2), public /media/download/**.
- **Deployment**: Docker + Kubernetes (giả định), port 8084 (context-path: /file).

*(Diagram mẫu - thay bằng Draw.io nếu cần. Dưới là Mermaid code, GitHub sẽ render tự động:)*

```mermaid
graph TD
    A[Client/App] -->|REST API| B["File-Service (Port 8084 /file)"]
    B -->|JWT Auth| D[CustomJwtDecoder]
    B -->|Upload/Validate| E[AWS Rekognition (analyze images)]
    B -->|Store Metadata| F[MongoDB (file_mgmt)]
    B -->|Persist File| G[AWS S3 (bucket: ${AWS_BUCKET_NAME})]
    B -->|Local Fallback| H[Local Storage (D:/upload)]
    I[Delete/By URL] --> B
    style B fill:#f9f,stroke:#333,stroke-width:2px
```

## ✨ Tính Năng Chính
- **File Upload**: Multipart files (max 10MB), store to S3 (with metadata to MongoDB), local fallback.
- **Download**: GET by fileName (byte[] or Resource, contentType header).
- **Delete**: POST by list of URLs (returns deleted keys).
- **Image Validation**: POST /s3/validate-many (multipart, returns ImageValidationResult: labels, confidence, isSafe).
- **Security**: PreAuthorize 'UPLOAD_FILE' for upload/validate, public download.
- **Tracking**: MongoDB metadata (md5 checksum, ownerId, size, path).
- **Error Handling**: Standardized ApiResponse, GlobalExceptionHandler (IOException, FeignException).

## 🛠️ Tech Stack
| Component          | Technology                  | Details                                      |
|--------------------|-----------------------------|----------------------------------------------|
| **Language/Framework** | Java 17+ / Spring Boot 3.x | REST Controllers, Multipart, Security        |
| **Database**       | MongoDB                     | file_mgmt (id, contentType, size, path, md5, ownerId) |
| **Storage**        | AWS S3                      | Upload/delete (region: ${AWS_REGION}, bucket: ${AWS_BUCKET_NAME}) |
| **Validation**     | AWS Rekognition             | Analyze images (confidence-threshold: 80%, labels) |
| **Security**       | Spring Security (OAuth2)    | JWT converter (roles/scopes), PreAuthorize UPLOAD_FILE |
| **Utils**          | Lombok, Jackson, AWS SDK    | DTOs (FileResponse, DeleteRequest, ImageValidationResult), DigestUtils (md5) |

## 🚀 Cài Đặt & Chạy
### Yêu Cầu
- Java 17+ / Maven 3.6+.
- Docker (cho MongoDB).
- Environment vars: `SPRING_DATA_MONGODB_URI` (mongodb://root:root@localhost:27017/file-service), AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_REGION, AWS_BUCKET_NAME (xem application.yml).

### Bước 1: Clone Repo
```bash
git clone https://github.com/shopping-ecommerce/file-service.git
cd file-service
```

### Bước 2: Setup Môi Trường
```bash
# Copy env files (nếu có example)
cp src/main/resources/application.yml.example application.yml

# Build project
mvn clean install

# Setup Docker services (MongoDB)
docker-compose up -d  # Sử dụng docker-compose.yml nếu có
```

### Bước 3: Chạy Service
```bash
# Run với Maven
mvn spring-boot:run

# Hoặc JAR
java -jar target/file-service-*.jar
```

- Port mặc định: **8084** (context: /file, e.g., http://localhost:8084/file/s3/upload).
- Test endpoints: Sử dụng Postman/Swagger (http://localhost:8084/file/swagger-ui.html nếu enable).

Ví dụ test upload:
```bash
curl -X POST http://localhost:8084/file/s3/upload \
  -H "Authorization: Bearer <jwt-token>" \
  -F "files=@/path/to/image.jpg"
```

### Bước 4: Test & Debug
```bash
# Run tests
mvn test

# Check logs (DEBUG cho AWS SDK/services)
tail -f logs/application.log  # Hoặc console
```

- Public: GET /download/{fileName} (no auth).
- Auth required: Upload/validate/delete.

## 📚 Tài Liệu
- **API Docs**: Sử dụng SpringDoc OpenAPI (Swagger UI tại `/swagger-ui.html`).
- **Endpoints** (base: /file):
  | Method | Endpoint                          | Description                  | Auth Required    |
  |--------|-----------------------------------|------------------------------|------------------|
  | POST   | `/s3/upload` (multipart files)    | Upload to S3                 | Yes (UPLOAD_FILE)|
  | GET    | `/download/{fileName}`            | Download file                | No               |
  | POST   | `/s3/delete`                      | Delete by URLs               | Yes              |
  | POST   | `/s3/validate-many` (multipart)   | Validate images (Rekognition)| Yes (UPLOAD_FILE)|
- **Deployment Guide**: Xem `docs/deploy.md` (Kubernetes manifests cho microservices).
- **Contributing Guide**: Xem `CONTRIBUTING.md`.

## 🤝 Đóng Góp
- Tuân thủ code style: Checkstyle, Lombok annotations.
- Test coverage >80% trước merge.
  Pull requests welcome! Báo issue nếu bug hoặc feature request.

## 📄 Giấy Phép
Dự án này được phân phối dưới giấy phép MIT. Xem file [LICENSE](LICENSE) để biết chi tiết.

## 👥 Liên Hệ
- Author: [Hồ Huỳnh Hoài Thịnh] ([@github-hohuynhhoaithinh](https://github.com/hohuynhhoaithinh))
- Email: [hohuynhhoaithinh@gmail.com]

---

*Cảm ơn bạn đã sử dụng File-Service! 🚀*