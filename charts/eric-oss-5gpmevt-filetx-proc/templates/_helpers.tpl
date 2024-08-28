{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-5gpmevt-filetx-proc.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
This helper defines the script for terminating the side car container by job container.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-sidecar-quit" }}
{{- if eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true" }}
curl -X POST http://127.0.0.1:15020/quitquitquit;
{{- end -}}
{{- end -}}

{{- define "eric-oss-5gpmevt-filetx-proc.imagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-5gpmevt-filetx-proc" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-5gpmevt-filetx-proc" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-5gpmevt-filetx-proc" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-5gpmevt-filetx-proc" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
              {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- $imagePath := printf "%s/%s/%s:%s" $registryUrl $repoPath $name $tag -}}
    {{- print (regexReplaceAll "[/]+" $imagePath "/") -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-5gpmevt-filetx-proc.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-5gpmevt-filetx-proc.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.labels" -}}
  {{- $standard := include "eric-oss-5gpmevt-filetx-proc.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-5gpmevt-filetx-proc.config-labels" . | fromYaml -}}
  {{- include "eric-oss-5gpmevt-filetx-proc.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.fsGroup.coordinated" -}}
{{- $fsGroupValue := 10000 -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if .Values.global.fsGroup.namespace -}}
          {{- if eq .Values.global.fsGroup.namespace true -}}
            # The 'default' defined in the Security Policy will be used.
          {{- else -}}
            {{- $fsGroupValue -}}
          {{- end -}}
        {{- else -}}
          {{- $fsGroupValue -}}
        {{- end -}}
      {{- end -}}
    {{- else -}}
      {{- $fsGroupValue -}}
    {{- end -}}
  {{- else -}}
    {{- $fsGroupValue -}}
  {{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-5gpmevt-filetx-proc.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Define role kind to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.securityPolicyRoleKind" -}}
{{- $rolekind := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
        {{- if .Values.global.securityPolicy.rolekind -}}
            {{- $rolekind = .Values.global.securityPolicy.rolekind -}}
            {{- if and (ne $rolekind "Role") (ne $rolekind "ClusterRole") -}}
              {{- printf "For global.securityPolicy.rolekind only \"Role\", \"ClusterRole\" or \"\" is allowed as values." | fail -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $rolekind -}}
{{- end -}}

{{/*
Define RoleName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.securityPolicyRolename" -}}
{{- $rolename := (include "eric-oss-5gpmevt-filetx-proc.name" .) -}}
{{- if .Values.securityPolicy -}}
    {{- if .Values.securityPolicy.rolename -}}
        {{- $rolename = .Values.securityPolicy.rolename -}}
    {{- end -}}
{{- end -}}
{{- $rolename -}}
{{- end -}}

{{/*
Define RolebindingName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.securityPolicy.rolebindingName" -}}
{{- $rolekind := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
        {{- if .Values.global.securityPolicy.rolekind -}}
            {{- $rolekind = .Values.global.securityPolicy.rolekind -}}
            {{- if (eq $rolekind "Role") -}}
               {{- print (include "eric-oss-5gpmevt-filetx-proc.serviceAccountName" .) "-r-" (include "eric-oss-5gpmevt-filetx-proc.securityPolicyRolename" .) "-sp" -}}
            {{- else if (eq $rolekind "ClusterRole") -}}
               {{- print (include "eric-oss-5gpmevt-filetx-proc.serviceAccountName" .) "-c-" (include "eric-oss-5gpmevt-filetx-proc.securityPolicyRolename" .) "-sp" -}}
            {{- end }}
        {{- end }}
    {{- end -}}
{{- end -}}
{{- end -}}


{{/*
Create container level annotations
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.container-annotations" }}
    {{- if .Values.appArmorProfile -}}
    {{- $appArmorValue := .Values.appArmorProfile.type -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-5gpmevt-filetx-proc: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064).
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-oss-5gpmevt-filetx-proc.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-5gpmevt-filetx-proc.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}


Create log control configmap name.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.log-control-configmap.name" }}
  {{- include "eric-oss-5gpmevt-filetx-proc.name" . | printf "%s-log-control-configmap" | quote }}
{{- end }}

Standard labels of Helm and Kubernetes
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.standard-labels" -}}
app.kubernetes.io/name: {{ include "eric-oss-5gpmevt-filetx-proc.name" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ include "eric-oss-5gpmevt-filetx-proc.version" . }}
helm.sh/chart: {{ include "eric-oss-5gpmevt-filetx-proc.chart" . }}
chart: {{ include "eric-oss-5gpmevt-filetx-proc.chart" . }}
{{- end -}}


{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.annotations" -}}
  {{- $productInfo := include "eric-oss-5gpmevt-filetx-proc.product-info" . | fromYaml -}}
  {{- $prometheusAnn := include "eric-oss-5gpmevt-filetx-proc.prometheus" . | fromYaml -}}
  {{- $config := include "eric-oss-5gpmevt-filetx-proc.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-5gpmevt-filetx-proc.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $prometheusAnn $config)) | trim }}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Prometheus Pod scrape annotations DR-D470223-010
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.prometheus-scrape-pod" -}}
prometheus.io/scrape-role: "pod"
prometheus.io/scrape-interval: {{ .Values.prometheus.interval | quote }}
{{- end -}}


{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.terminationGracePeriodSeconds" -}}
  {{- if .Values.terminationGracePeriodSeconds -}}
    {{- toYaml .Values.terminationGracePeriodSeconds -}}
  {{- end -}}
{{- end -}}

{{/*
Define tolerations to comply with DR-D1120-060
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.tolerations" -}}
{{- $global := (list) -}}
{{- if (.Values.global).tolerations -}}
  {{- $global = .Values.global.tolerations -}}
{{- end -}}
{{- $local := (list) -}}
{{- if eq (typeOf .Values.tolerations) ("[]interface {}") -}}
  {{- $local = .Values.tolerations -}}
{{- else if (.Values.tolerations) -}}
  {{- $local = .Values.tolerations -}}
{{- end -}}
{{- $merged := (list) -}}
{{- if $global -}}
    {{- $merged = $global -}}
{{- end -}}
{{- if $local -}}
  {{- range $i, $localToleration := $local -}}
    {{- $localValue := get $localToleration "key" -}}
    {{- range $g, $globalToleration := $merged -}}
      {{- $globalValue := get $globalToleration "key" -}}
      {{- if eq $localValue $globalValue -}}
        {{- $merged = without $merged $globalToleration -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $merged = concat $merged $local -}}
{{- end -}}
{{- if $merged -}}
    {{- toYaml $merged -}}
{{- end -}}
{{- /* Do nothing if both global and local groups are not set */ -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    nodeSelector: {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    nodeSelector: {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{/*
Define JVM heap size (DR-D1126-010 | DR-D1126-011)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-5gpmevt-filetx-proc is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "limits" "memory") | float64) 1000) | float64  -}}
    {{- end -}}

    {{- if (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "initialMemoryAllocationPercentage" | float64 -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%f" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM =index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "smallMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = index .Values "resources" "eric-oss-5gpmevt-filetx-proc" "jvm" "largeMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}
{{- printf "%s %s" $initRAM $maxRAM -}}
{{- end -}}

{/*
Define network policy pod selector rules for ingress DR-D1125-059, DR-D1125-050, DR-D1125-052, DR-D1125-054
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.netpol-ingress-pod-selector-rules" -}}
    {{- if (index .Values "networkPolicy") -}}
        {{- if (index .Values "networkPolicy" "podSelector") -}}
- from:
            {{- range (index .Values "networkPolicy" "podSelector") }}
                {{- $label := .label -}}
                {{- if .ingress -}}
                    {{- range .ingress }}
                        {{- if .value }}
  - podSelector:
      matchLabels:
        {{ $label }}: {{ .value }}
                        {{- end -}}
                    {{- end -}}
                {{- end -}}
            {{- end -}}
            {{- range (index .Values "networkPolicy" "podSelector") }}
                {{- $label := .label -}}
                {{- if .ingress }}
  ports:
                    {{- range .ingress }}
    - port: {{ .port }}
      protocol: {{ .protocol }}
                    {{- end -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}


{/*
Define network policy egress port rules for egress DR-D1125-059, DR-D1125-050, DR-D1125-052, DR-D1125-054
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.netpol-egress-port-rules" -}}
    {{- if (index .Values "networkPolicy") -}}
        {{- if (index .Values "networkPolicy" "port") -}}
            {{- if (index .Values "networkPolicy" "port" "egress") -}}
- to:
  - ipBlock:
      cidr: 0.0.0.0/0
  ports:
                {{- range (index .Values "networkPolicy" "port" "egress") }}
  - port: {{ .port }}
    protocol: {{ .protocol }}
                {{- end -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}

{{/*
Define the log streaming method (DR-470222-010)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- if .Values.global -}}
  {{- if .Values.global.log -}}
      {{- if .Values.global.log.streamingMethod -}}
        {{- $streamingMethod = .Values.global.log.streamingMethod }}
      {{- else if empty .Values.global.log.streamingMethod }}
        {{- $streamingMethod = "direct" -}}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-5gpmevt-filetx-proc.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-5gpmevt-filetx-proc.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-5gpmevt-filetx-proc.streamingMethod" .) -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod }}
- name: LOG_CTRL_FILE
  value: "{{ .Values.log.logControlFile }}"
  {{- end -}}
  {{- if index .Values "log" "runtime-level-control" -}}
    {{- if index .Values "log" "runtime-level-control" "enabled" }}
- name: RUN_TIME_LEVEL_CONTROL
  value: "{{ index .Values "log" "runtime-level-control" "enabled" }}"
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) -}}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-http.xml"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual.xml"
  {{- end }}
- name: LOGSTASH_DESTINATION
  value: eric-log-transformer
- name: LOGSTASH_PORT
  value: "9080"
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-5gpmevt-filetx-proc.name
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}

{{/*
    Define Image Pull Policy, DR-D1121-102
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry" -}}
            {{- if index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry" "imagePullPolicy" -}}
                {{- $globalRegistryPullPolicy = index .Values "imageCredentials" "eric-oss-5gpmevt-filetx-proc" "registry" "imagePullPolicy" -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}


{{/*----------------------------------- Service mesh functions ----------------------------------*/}}

{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-inject" }}
{{- if eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}


{{- define "eric-oss-5gpmevt-filetx-proc.istio-proxy-config-annotation" }}
{{- if eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true" }}
proxy.istio.io/config: '{ "holdApplicationUntilProxyStarts": true }'
{{- end -}}
{{- end -}}


{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-version" }}
{{- if eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{ .Values.global.serviceMesh.annotations | toYaml }}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
check global.security.tls.enabled
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
      {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
      {{- "false" -}}
    {{- end -}}
  {{- else -}}
    {{- "false" -}}
  {{- end -}}
{{- else -}}
  {{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
This helper defines which out-mesh services will be reached by this one.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-ism2osm-labels" -}}
{{- if eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-5gpmevt-filetx-proc.global-security-tls-enabled" .) "true" }}
eric-oss-dmm-kf-op-sz-kafka-ism-access: "true"
  {{- end }}
{{- end }}
{{- end -}}

{{/*
Define kafka bootstrap server
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.kafka-bootstrap-server" -}}
{{- $kafkaBootstrapServer := "" -}}
{{- $serviceMesh := ( include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" . ) -}}
{{- $tls := ( include "eric-oss-5gpmevt-filetx-proc.global-security-tls-enabled" . ) -}}
{{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
    {{- $kafkaBootstrapServer = .Values.spring.kafka.bootstrapServersTls -}}
{{ else }}
    {{- $kafkaBootstrapServer = .Values.spring.kafka.bootstrapServer -}}
{{ end }}
{{- if .Values.global -}}
  {{- if .Values.global.dependentServices -}}
    {{- if .Values.global.dependentServices.dmm -}}
        {{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
               {{- $kafkaBootstrapServer = index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
           {{- end -}}
        {{ else }}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
               {{- $kafkaBootstrapServer = index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
           {{- end -}}
        {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $kafkaBootstrapServer -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume.
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-volume" -}}
{{- if and (eq (include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" .) "true") (eq (include "eric-oss-5gpmevt-filetx-proc.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"{{ include "eric-oss-5gpmevt-filetx-proc.name" . }}-kafka-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-5gpmevt-filetx-proc.name" . }}-kafka-secret","optional":true}},"{{ include "eric-oss-5gpmevt-filetx-proc.name" . }}-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-oss-5gpmevt-filetx-proc.name" . }}-kafka-certs-tls":{"mountPath":"/etc/istio/tls/eric-oss-dmm-kf-op-sz-kafka-bootstrap/","readOnly":true},"{{ include "eric-oss-5gpmevt-filetx-proc.name" . }}-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{/*
Create kafka service issuer reference , overriding from parent chart
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.kafkaIssuerReference" -}}
    {{- $kafkaIssuerReference := .Values.spring.kafka.issuerReference -}}
    {{- if .Values.global -}}
        {{- if .Values.global.dependentServices -}}
            {{- if .Values.global.dependentServices.dmm -}}
                {{- if index .Values.global.dependentServices.dmm "kafka-issuerReference" -}}
                    {{- $kafkaIssuerReference = index .Values.global.dependentServices.dmm "kafka-issuerReference" -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $kafkaIssuerReference -}}
{{- end -}}

{{/*
    Define supplementalGroups (DR-D1123-135)
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.supplementalGroups" -}}
  {{- $globalGroups := (list) -}}
  {{- if ( (((.Values).global).podSecurityContext).supplementalGroups) }}
    {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $localGroups := (list) -}}
  {{- if ( ((.Values).podSecurityContext).supplementalGroups) -}}
    {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $mergedGroups := (list) -}}
  {{- if $globalGroups -}}
    {{- $mergedGroups = $globalGroups -}}
  {{- end -}}
  {{- if $localGroups -}}
    {{- $mergedGroups = concat $globalGroups $localGroups | uniq -}}
  {{- end -}}
  {{- if $mergedGroups -}}
    supplementalGroups: {{- toYaml $mergedGroups | nindent 8 -}}
  {{- end -}}
  {{- /*Do nothing if both global and local groups are not set */ -}}
{{- end -}}


{{/*
Create Service Mesh Egress enabling option
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-egress-enabled" }}
  {{- $globalMeshEgressEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.egress -}}
        {{- $globalMeshEgressEnabled = .Values.global.serviceMesh.egress.enabled -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEgressEnabled -}}
{{- end -}}

{{/*
This helper defines permissive network policy for external access
*/}}
{{- define "eric-oss-5gpmevt-filetx-proc.service-mesh-egress-gateway-access-label" }}
{{- $serviceMesh := include "eric-oss-5gpmevt-filetx-proc.service-mesh-enabled" . | trim -}}
{{- $serviceMeshEgress := include "eric-oss-5gpmevt-filetx-proc.service-mesh-egress-enabled" . | trim -}}
{{- if and (eq $serviceMesh "true") (eq $serviceMeshEgress "true") -}}
service-mesh-egress-gateway-access: "true"
{{- end -}}
{{- end -}}

{{- define "eric-oss-5gpmevt-filetx-proc.kafka-cluster-label" }}
strimzi.io/cluster: {{  .Values.spring.kafka.clusterName  }}
{{- end -}}

{{- define "eric-oss-5gpmevt-filetx-proc.egress-bandwidth-label" }}
    {{- if .Values.bandwidth.maxEgressRate }}
        app.kubernetes.io/egress-bandwidth: {{ .Values.bandwidth.maxEgressRate | quote }}
    {{- end }}
{{- end -}}