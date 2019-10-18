package com.mcs.mergeminder.gitlab

import com.mcs.mergeminder.properties.GitlabProperties
import org.gitlab4j.api.models.MergeRequest
import spock.lang.Specification
import spock.lang.Unroll

class GitlabIntegrationSpec extends Specification {

    @Unroll
    def 'ignore merge request when: #scenario'() {
        given: 'configured list of labels by which to ignore'
        GitlabProperties gitlabProperties = Mock(GitlabProperties) {
            getIgnoredByLabels() >> ignoredByLabels
        }
        def gitlabIntegration = new GitlabIntegration(gitlabProperties)
        def mr = new MergeRequest(
                labels: mergeRequestLabels
        )

        when: 'merge request containing a label has ignored label'
        def shallIgnore = gitlabIntegration.ignoreMergeRequest(mr)

        then:
        shallIgnore == expectedToIgnore

        where:
        ignoredByLabels   | mergeRequestLabels           | expectedToIgnore | scenario
        ['ignored-label'] | ['label-1', 'ignored-label'] | true             | 'merge request has a label that matches to one configured ignoredByLabels'
        []                | ['label-1', 'ignored-label'] | false            | 'merge request has a labels, but there are no configured ignoredByLabels'
        null              | ['label-1', 'ignored-label'] | false            | 'merge request has labels, ignoredByLabels is null'
        ['ignored-label'] | []                           | false            | 'merge request has no labels'
        ['ignored-label'] | null                         | false            | 'merge request has no labels - null'
        ['1', '5', 's ']  | [' 10', ' s', '99']          | true             | 'merge request has multiple labels and there are multiple configured ignoredByLabels - ignored'
        ['1', '5', 's ']  | [' 10', '22', '99']          | false            | 'merge request has multiple labels and there are multiple configured ignoredByLabels - not ignored'
    }
}
