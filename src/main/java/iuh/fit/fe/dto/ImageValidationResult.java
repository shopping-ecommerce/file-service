package iuh.fit.fe.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageValidationResult {
    // index 0-based để dùng trong code
    private int index;
    // position 1-based cho người dùng (ảnh thứ mấy)
    private int position;
    private String filename;

    private boolean passed;           // true = hợp lệ
    private String reason;            // lý do nếu failed (null nếu passed)
    private List<String> blockedLabels; // các nhãn bị cấm match (nếu failed)
}
