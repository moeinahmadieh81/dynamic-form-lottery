package com.example.dynamicform.form.infrastructure.persistence;

import com.example.dynamicform.form.domain.FormStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormEntityLifecycleTest {

    @Test
    void movesThroughLotteryLifecycle() {
        FormEntity form = FormEntity.draft("Lottery", null, null, null);

        form.publish();
        assertThat(form.getStatus()).isEqualTo(FormStatus.PUBLISHED);

        form.close();
        assertThat(form.getStatus()).isEqualTo(FormStatus.CLOSED);

        form.markDrawn();
        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAWN);
    }

    @Test
    void cannotCloseDraftForm() {
        FormEntity form = FormEntity.draft("Lottery", null, null, null);

        assertThatThrownBy(form::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }
}
