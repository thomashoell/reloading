package de.tho.reloading.web;

import java.util.UUID;

/** Row of the list of case batches; everything is formatted for display. */
public record ProcessedCaseRow(UUID id, String processedOn, String casing, String lotNumber, int quantity,
                               int timesFired, String rifle, String caseLength, boolean trimmed, String calibration) {
}
