## Summary

<!-- One paragraph describing what this PR does and why. -->

## Related issue

Closes #

## Changes

<!-- Bullet list of what was added, changed, or removed. -->

-

## Testing

**`./gradlew ktlintCheck` passes:** yes / no
**`./gradlew detekt` passes:** yes / no
**`./gradlew test` passes:** yes / no
**`./gradlew build` passes:** yes / no
**`./gradlew verifyPlugin` passes when relevant:** yes / no / not applicable

### Manual smoke (required for open/carry-over changes)

<!-- If your change touches WorktreeCarryOverService, WorktreeOpenService, or any
     action that triggers a project open, run `./gradlew runIde`, exercise the
     affected create/open/carry-over flow, and describe what you tested and
     observed. Delete this section if not applicable. -->

- IDE and version tested:
- Scenario:
- Observed result:

## Checklist

- [ ] `./gradlew ktlintCheck detekt test build` passes locally
- [ ] `./gradlew verifyPlugin` passes against both configured targets when the change affects platform APIs, dependencies, plugin metadata, or distribution behavior
- [ ] No new unresolved placeholder markers introduced
- [ ] Carry-over / open flows manually smoked (or not applicable)
- [ ] PR description is complete and the related issue is linked
