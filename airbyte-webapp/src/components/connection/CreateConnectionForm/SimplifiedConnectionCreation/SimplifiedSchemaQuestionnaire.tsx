import { ComponentProps, useEffect, useMemo, useState } from "react";
import { useFormContext } from "react-hook-form";
import { FormattedMessage } from "react-intl";

import { FormConnectionFormValues } from "components/connection/ConnectionForm/formConfig";
import { FormFieldLayout } from "components/connection/ConnectionForm/FormFieldLayout";
import { RadioButtonTiles } from "components/connection/CreateConnection/RadioButtonTiles";
import { updateStreamSyncMode } from "components/connection/syncCatalog/SyncCatalog/updateStreamSyncMode";
import { SyncModeValue } from "components/connection/syncCatalog/SyncModeSelect";
import { ControlLabels } from "components/LabeledControl";
import { Badge } from "components/ui/Badge";
import { FlexContainer } from "components/ui/Flex";
import { Icon } from "components/ui/Icon";
import { Text } from "components/ui/Text";

import { DestinationSyncMode, SyncMode } from "core/api/types/AirbyteClient";
import { useConnectionFormService } from "hooks/services/ConnectionForm/ConnectionFormService";

import styles from "./SimplifiedSchemaQuestionnaire.module.scss";

type Delivery = "mirrorSource" | "appendChanges";
type IncrementOrRefresh = SyncMode;
type QuestionnaireOutcomes = Record<Delivery, Array<[SyncMode, DestinationSyncMode]>>;

const deliveryOptions: ComponentProps<typeof RadioButtonTiles<Delivery>>["options"] = [
  {
    value: "mirrorSource",
    label: "connectionForm.questionnaire.delivery.mirrorSource.title",
    labelValues: {
      badge: (
        <Badge variant="blue" className={styles.badgeAlignment}>
          Recommended
        </Badge>
      ),
    },
    description: "connectionForm.questionnaire.delivery.mirrorSource.subtitle",
  },
  {
    value: "appendChanges",
    label: "connectionForm.questionnaire.delivery.appendChanges.title",
    description: "connectionForm.questionnaire.delivery.appendChanges.subtitle",
  },
];

const deletionRecordsOptions: ComponentProps<typeof RadioButtonTiles<IncrementOrRefresh>>["options"] = [
  {
    value: SyncMode.incremental,
    label: "connectionForm.questionnaire.incrementOrRefresh.increment.title",
    description: "connectionForm.questionnaire.incrementOrRefresh.increment.subtitle",
  },
  {
    value: SyncMode.full_refresh,
    label: "connectionForm.questionnaire.incrementOrRefresh.refresh.title",
    description: "connectionForm.questionnaire.incrementOrRefresh.refresh.subtitle",
    extra: (
      <Text color="blue" size="sm">
        <FlexContainer alignItems="flex-end" gap="xs">
          <Icon type="warningOutline" size="sm" />
          <FormattedMessage id="connectionForm.questionnaire.incrementOrRefresh.refresh.warning" />
        </FlexContainer>
      </Text>
    ),
  },
];

export const pruneUnsupportedModes = (
  modes: Array<[SyncMode, DestinationSyncMode]>,
  supportedSyncModes: SyncMode[],
  supportedDestinationSyncModes: DestinationSyncMode[] | undefined
) => {
  return modes.filter(([syncMode, destinationSyncMode]) => {
    return supportedSyncModes.includes(syncMode) && supportedDestinationSyncModes?.includes(destinationSyncMode);
  });
};

export const getEnforcedDelivery = (outcomes: QuestionnaireOutcomes): Delivery | undefined => {
  if (outcomes.mirrorSource.length === 0) {
    // there are no mirrorSource choices; return appendChanges if present otherwise choice valid
    return outcomes.appendChanges.length > 0 ? "appendChanges" : undefined;
  } else if (outcomes.appendChanges.length === 0) {
    return "mirrorSource"; // has mirrorSource but no appendChanges, pre-select mirrorSource
  } else if (
    outcomes.mirrorSource.length === 1 &&
    outcomes.appendChanges.length === 1 &&
    outcomes.mirrorSource[0][0] === outcomes.appendChanges[0][0] &&
    outcomes.mirrorSource[0][1] === outcomes.appendChanges[0][1]
  ) {
    // has mirrorSource and has appendChanges; both are [length=1] and have the same SyncMode
    return "mirrorSource"; // which value is returned doesn't matter, so mirrorSource it is
  }

  // multiple options, pre-select nothing so user can choose
  return undefined;
};

export const getEnforcedIncrementOrRefresh = (supportedSyncModes: SyncMode[]) => {
  return supportedSyncModes.length === 1 ? supportedSyncModes[0] : undefined;
};

export const SimplifiedSchemaQuestionnaire = () => {
  const {
    connection,
    destDefinitionSpecification: { supportedDestinationSyncModes },
  } = useConnectionFormService();

  const supportedSyncModes: SyncMode[] = useMemo(() => {
    const foundModes = new Set<SyncMode>();
    for (let i = 0; i < connection.syncCatalog.streams.length; i++) {
      const stream = connection.syncCatalog.streams[i];
      stream.stream?.supportedSyncModes?.forEach((mode) => foundModes.add(mode));
    }
    return Array.from(foundModes);
  }, [connection.syncCatalog.streams]);

  const questionnaireOutcomes = useMemo<QuestionnaireOutcomes>(
    () => ({
      mirrorSource: pruneUnsupportedModes(
        [
          [SyncMode.incremental, DestinationSyncMode.append_dedup],
          [SyncMode.full_refresh, DestinationSyncMode.overwrite],
          [SyncMode.incremental, DestinationSyncMode.append],
        ],
        supportedSyncModes,
        supportedDestinationSyncModes
      ),
      appendChanges: pruneUnsupportedModes(
        [
          [SyncMode.incremental, DestinationSyncMode.append],
          [SyncMode.full_refresh, DestinationSyncMode.append],
        ],
        supportedSyncModes,
        supportedDestinationSyncModes
      ),
    }),
    [supportedSyncModes, supportedDestinationSyncModes]
  );

  const enforcedSelectedDelivery = getEnforcedDelivery(questionnaireOutcomes);
  const enforcedIncrementOrRefresh = getEnforcedIncrementOrRefresh(supportedSyncModes);

  const [selectedDelivery, _setSelectedDelivery] = useState<Delivery | undefined>(enforcedSelectedDelivery);
  const [selectedIncrementOrRefresh, setSelectedIncrementOrRefresh] = useState<IncrementOrRefresh | undefined>(
    enforcedIncrementOrRefresh
  );

  const setSelectedDelivery: typeof _setSelectedDelivery = (value) => {
    _setSelectedDelivery(value);
    if (value === "mirrorSource") {
      // clear any user-provided answer for the second question when switching to mirrorSource
      // this is purely a UX decision
      setSelectedIncrementOrRefresh(enforcedIncrementOrRefresh);
    }
  };

  const selectedModes = useMemo<SyncModeValue[]>(() => {
    if (selectedDelivery === "mirrorSource") {
      return questionnaireOutcomes.mirrorSource.map(([syncMode, destinationSyncMode]) => {
        return {
          syncMode,
          destinationSyncMode,
        };
      });
    } else if (selectedDelivery === "appendChanges" && selectedIncrementOrRefresh) {
      return [
        {
          syncMode: selectedIncrementOrRefresh,
          destinationSyncMode: DestinationSyncMode.append,
        },
      ];
    }
    return [];
  }, [selectedDelivery, questionnaireOutcomes.mirrorSource, selectedIncrementOrRefresh]);

  // if a source & destination sync mode selection has been made (by default or by the user), show the result
  let selectionMessage;
  if (selectedModes.length) {
    selectionMessage = <FormattedMessage id="connectionForm.questionnaire.result" />;
  }

  // when a sync mode is selected, choose it for all streams
  const { trigger, getValues, setValue } = useFormContext<FormConnectionFormValues>();
  useEffect(() => {
    if (!selectedModes.length) {
      return;
    }

    const currentFields = getValues("syncCatalog.streams");
    const nextFields = currentFields.map((field) => {
      for (let i = 0; i < selectedModes.length; i++) {
        const { syncMode, destinationSyncMode } = selectedModes[i];

        if (field?.stream && field?.config) {
          if (!field.stream?.supportedSyncModes?.includes(syncMode)) {
            continue;
          }

          const nextConfig = updateStreamSyncMode(field.stream, field.config, {
            syncMode,
            destinationSyncMode,
          });
          return {
            ...field,
            config: nextConfig,
          };
        }
      }
      return field;
    });
    setValue("syncCatalog.streams", nextFields);
    trigger("syncCatalog.streams");
  }, [setValue, trigger, getValues, selectedDelivery, selectedIncrementOrRefresh, selectedModes]);

  const showSecondQuestion = enforcedIncrementOrRefresh == null && selectedDelivery === "appendChanges";

  return (
    <FlexContainer
      direction="column"
      gap={showSecondQuestion ? "xl" : "md" /* maintain consistent spacing between the first question & message */}
    >
      {enforcedSelectedDelivery == null && (
        <FormFieldLayout alignItems="flex-start" nextSizing>
          <ControlLabels
            label={
              <FlexContainer direction="column">
                <Text as="div">
                  <FormattedMessage id="connectionForm.questionnaire.delivery" />
                </Text>
              </FlexContainer>
            }
          />
          <RadioButtonTiles
            direction="column"
            name="delivery"
            options={deliveryOptions}
            selectedValue={selectedDelivery ?? ""}
            onSelectRadioButton={setSelectedDelivery}
          />
        </FormFieldLayout>
      )}

      <div className={showSecondQuestion ? styles.expandedQuestion : styles.collapsedQuestion}>
        <FormFieldLayout alignItems="flex-start" nextSizing>
          <ControlLabels
            label={
              <FlexContainer direction="column">
                <Text>
                  <FormattedMessage id="connectionForm.questionnaire.incrementOrRefresh" />
                </Text>
              </FlexContainer>
            }
          />
          <RadioButtonTiles
            direction="column"
            name="delectedRecords"
            options={deletionRecordsOptions}
            selectedValue={selectedIncrementOrRefresh ?? ""}
            onSelectRadioButton={setSelectedIncrementOrRefresh}
          />
        </FormFieldLayout>
      </div>

      {selectionMessage && (
        <FormFieldLayout alignItems="flex-start" nextSizing>
          <ControlLabels label="" />
          <Text color="blue">{selectionMessage}</Text>
        </FormFieldLayout>
      )}
    </FlexContainer>
  );
};
