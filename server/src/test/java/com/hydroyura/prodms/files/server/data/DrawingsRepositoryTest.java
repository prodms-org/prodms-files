package com.hydroyura.prodms.files.server.data;

import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_CMD_CREATE_FILE;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_CMD_GET_TAGS;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_CMD_PLACE_FILE_TO_BUCKET;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_CMD_SET_TAG;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_DOCKER_IMAGE;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_DRAWING_BUCKET;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_PWD;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_CONTAINER_NAME;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_CONTENT_1;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_CONTENT_2;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_CONTENT_3;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_NAME_1;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_NAME_2;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_NAME_3;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_TEST_FILE_NAME_PREDICATE;
import static com.hydroyura.prodms.files.server.data.DrawingRepositoryTestUnits.MINIO_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hydroyura.prodms.files.server.api.enums.DrawingType;
import io.minio.MinioClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;


class DrawingsRepositoryTest {

    private final DrawingsRepository repository;

    @ClassRule
    public static MinIOContainer TEST_CONTAINER =
        new MinIOContainer(MINIO_DOCKER_IMAGE)
            .withUserName(MINIO_USER)
            .withPassword(MINIO_PWD)
            .withCreateContainerCmdModifier(cmd -> cmd.withName(MINIO_TEST_CONTAINER_NAME));

    @BeforeAll
    static void startContainer() throws Exception {
        TEST_CONTAINER.start();
        TEST_CONTAINER.execInContainer("bash", "-c",
            "mc config host add minio-test http://localhost:9000 %s %s".formatted(MINIO_USER, MINIO_PWD));
        TEST_CONTAINER.execInContainer("bash", "-c",
            "mc mb --with-versioning minio-test/%s".formatted(MINIO_DRAWING_BUCKET));
    }

    @AfterAll
    static void closeContainer() {
        TEST_CONTAINER.close();
    }


    @AfterEach
    void clearBucket() throws Exception {
        TEST_CONTAINER.execInContainer("bash", "-c",
            "mc rm --recursive --force minio-test/%s/".formatted(MINIO_DRAWING_BUCKET));
    }

    DrawingsRepositoryTest() {
        var minioClient = MinioClient.builder()
            .endpoint(TEST_CONTAINER.getS3URL())
            .credentials(TEST_CONTAINER.getUserName(), TEST_CONTAINER.getPassword())
            .build();
        this.repository = new DrawingsRepository(minioClient, MINIO_DRAWING_BUCKET);
    }

    @Test
    void getDrawingUrl_FOR_NOT_EXISTED_FILE() {
        var result = repository.getDrawingUrl(MINIO_TEST_FILE_NAME_1);
        assertTrue(result.isEmpty());
    }

    @Test
    void getDrawingUrl_FOR_EXISTED_FILE() throws Exception {
        // given
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_1, MINIO_TEST_FILE_CONTENT_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_1, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1)
        );

        // when
        var result = repository.getDrawingUrl(MINIO_TEST_FILE_NAME_1);

        // then
        assertTrue(result.isPresent());
    }


    @Test
    void listObjects_GET_ONLY_LATEST() throws Exception {
        // given
        // file 1
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_1, MINIO_TEST_FILE_CONTENT_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_1, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_SET_TAG.formatted(MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1, "type", "SI")
        );
        // file 2
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_2, MINIO_TEST_FILE_CONTENT_2)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_2, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_2)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_SET_TAG.formatted(MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_2, "type", "AS")
        );
        // file 1 new version
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_1, MINIO_TEST_FILE_CONTENT_3)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_1, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_SET_TAG.formatted( MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1, "type", "SI")
        );
        // when
        var result = repository.listObjects(MINIO_TEST_FILE_NAME_PREDICATE);

        // then
        assertEquals(2, result.size());
        //assertTrue(result.containsAll(List.of(MINIO_TEST_FILE_NAME_1, MINIO_TEST_FILE_NAME_2)));
    }

    @Test
    void listObjects_FIND_ONLY_WITH_PREFIX() throws Exception {
        // given
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_1, MINIO_TEST_FILE_CONTENT_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_1, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_SET_TAG.formatted( MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_1, "type", "SI")
        );
        // file 3
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_CREATE_FILE.formatted(MINIO_TEST_FILE_NAME_3, MINIO_TEST_FILE_CONTENT_2)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_PLACE_FILE_TO_BUCKET.formatted(MINIO_TEST_FILE_NAME_3, MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_3)
        );
        TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_SET_TAG.formatted( MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_3, "type", "SI")
        );
        // when
        var result = repository.listObjects(MINIO_TEST_FILE_NAME_PREDICATE);

        // then
        assertEquals(1, result.size());
    }


    @Test
    void addFile_SUCCESS() throws Exception {
        repository.addFile(
            MINIO_TEST_FILE_NAME_2,
            Map.of("type", DrawingType.SIMPLE.getCode()),
            MINIO_TEST_FILE_CONTENT_2.getBytes(StandardCharsets.UTF_8)
        );

        var result = TEST_CONTAINER.execInContainer(
            "bash",
            "-c",
            MINIO_CMD_GET_TAGS.formatted(MINIO_DRAWING_BUCKET, MINIO_TEST_FILE_NAME_2)
        );

        assertEquals(0, result.getExitCode());

        Optional<String> type = Arrays
            .stream(result.getStdout().split("\\n"))
            .filter(s -> s.contains("type :"))
            .map(s -> s.split(" : ")[1])
            .findFirst();
        assertTrue(type.isPresent() && type.get().equals("SI"));
    }

}