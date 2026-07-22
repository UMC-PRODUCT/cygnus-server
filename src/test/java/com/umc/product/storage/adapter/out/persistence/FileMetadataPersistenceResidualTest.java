package com.umc.product.storage.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.storage.domain.FileMetadata;

@DisplayName("FileMetadataPersistenceAdapter")
class FileMetadataPersistenceResidualTest {

    @Test
    @DisplayName("단건·batch·존재·저장·삭제를 repository에 위임한다")
    void 모든_계약을_위임한다() {
        FileMetadataRepository repository = mock(FileMetadataRepository.class);
        FileMetadataPersistenceAdapter sut = new FileMetadataPersistenceAdapter(repository);
        FileMetadata metadata = mock(FileMetadata.class);
        List<FileMetadata> values = List.of(metadata);
        given(repository.findById("id")).willReturn(Optional.of(metadata));
        given(repository.findByIdIn(List.of("id"))).willReturn(values);
        given(repository.existsById("id")).willReturn(true);
        given(repository.save(metadata)).willReturn(metadata);

        assertThat(sut.findByFileId("id")).contains(metadata);
        assertThat(sut.findByFileIds(List.of("id"))).isSameAs(values);
        assertThat(sut.existsByFileId("id")).isTrue();
        assertThat(sut.save(metadata)).isSameAs(metadata);
        sut.deleteByFileId("id");
        then(repository).should().deleteById("id");
    }
}
