package co.istad.ai_interview_app.features.admin.domain;

import co.istad.ai_interview_app.shared.config.audit.BaseEntity;
import co.istad.ai_interview_app.shared.domain.UserAccount;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "system_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_system_settings_setting_key",
                        columnNames = "setting_key"
                )
        }
)
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 150)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SettingValueType valueType = SettingValueType.STRING;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean editable = true;

    @ManyToOne
    @JoinColumn(name = "updated_by_user_account_id")
    private UserAccount updatedByUserAccount;
}