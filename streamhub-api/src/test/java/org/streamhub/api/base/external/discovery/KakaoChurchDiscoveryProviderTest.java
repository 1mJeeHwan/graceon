package org.streamhub.api.base.external.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Table-based tests for {@link KakaoChurchDiscoveryProvider#isRelevantChurch} — the church-vs-noise
 * filter. Cases mirror real Kakao keyword-search results observed near Seoul City Hall, where the
 * "교회" sweep pulls in parking lots, education halls, denomination HQs, and memorial halls.
 */
class KakaoChurchDiscoveryProviderTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" → {2}")
    @CsvSource(delimiter = '|', value = {
        // keep — real, visitable churches/cathedrals
        "강남가정교회                       | 종교,개신교,교회   | true",
        "정동제일교회                       | 종교,개신교,교회   | true",
        "여의도순복음교회 남대문성전         | 종교,개신교,교회   | true",
        "천주교남대문시장성당 준본당         | 종교,천주교,성당   | true",
        "한국천부교전도관유지재단서대문교회   | 종교              | true",
        // drop — annex / office / parking / facility noise
        "새문안교회 주차장                   | 교통,주차장        | false",
        "영락교회 지하주차장 입구             | 교통,주차장        | false",
        "내수동교회 교육관                   | 종교,개신교,교회   | false",
        "감리교회 총회본부                   | 종교,개신교        | false",
        "기독교대한감리회 교회학교전국연합회   | 종교,개신교        | false",
        "승동교회 130주년기념관              | 종교,개신교        | false",
        "영락교회 영락주간보호센터           | 사회복지          | false",
        "영락교회 봉사관                     | 종교,개신교        | false",
        "중앙연회                           | 종교,개신교,교회   | false",
        // drop — not a church at all
        "스타벅스 강남점                     | 음식점,카페        | false",
    })
    void classifiesChurchVsNoise(String name, String category, boolean expected) {
        assertThat(KakaoChurchDiscoveryProvider.isRelevantChurch(name, category)).isEqualTo(expected);
    }

    @Test
    void blankOrNullName_isNotChurch() {
        assertThat(KakaoChurchDiscoveryProvider.isRelevantChurch(null, "종교")).isFalse();
        assertThat(KakaoChurchDiscoveryProvider.isRelevantChurch("  ", "종교,개신교,교회")).isFalse();
    }
}
