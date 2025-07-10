package csw.fcfs.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 최적화된 JsonMapper 빌더 사용
        JsonMapper mapper = JsonMapper.builder()
                // 표준 모듈들 등록
                .addModule(new JavaTimeModule())      // Java 8 시간 타입 지원 (Instant, LocalDateTime 등)
                .addModule(new Jdk8Module())          // Optional 및 JDK8 기능 지원
                .addModule(new ParameterNamesModule())// 생성자 파라미터 이름 인트로스펙션
                .addModule(new BlackbirdModule())     // 성능 최적화 모듈 (JDK 11+)

                // BigDecimal 동작: 후행 0 제거 비활성화
                .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)

                // Mapper 기능들
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(MapperFeature.USE_STD_BEAN_NAMING)

                // Parser 기능들
                .enable(JsonParser.Feature.ALLOW_COMMENTS)       // JSON 주석 허용
                .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)   // 기본 스트림 자동 닫기 비활성화

                // 직렬화 기능들
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO 8601 문자열로 날짜 출력
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)       // 빈 Bean 직렬화 허용

                // 역직렬화 기능들
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 알 수 없는 속성 무시

                // 보안: 다형성 역직렬화 비활성화 (의도적으로 사용하지 않는 한)
//                .deactivateDefaultTyping()

                // 가시성: 필드만 직렬화, getter/setter 무시
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

                // 출력 스트림 자동 닫기 비활성화
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)

                // null이 아닌 값만 포함
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)

                // 일관된 날짜/시간 형식 제공 (UTC, 시간대 오프셋에 콜론 포함)
                .defaultDateFormat(new StdDateFormat()
                        .withColonInTimeZone(true)
                        .withTimeZone(TimeZone.getTimeZone("UTC"))
                )
                .build();

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        mapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Spring Data의 Page 객체를 위한 MixIn 추가 (페이지네이션 최적화)
        mapper.addMixIn(Page.class, PageMixIn.class);

        return mapper;
    }

    // Page 객체의 일부 내부 속성 무시를 위한 MixIn
    @JsonIgnoreProperties(value = {"pageable.sort.sorted", "pageable.sort.unsorted"}, allowGetters = true)
    interface PageMixIn {
    }
}
