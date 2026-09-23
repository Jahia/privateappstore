# Changelog

## 5.1.2 (unreleased)

### Security

- **SEC-381: an upload grants the `owner` role only on the nodes it creates.** Before this
  change, a store developer who uploaded a new version of a module (or package) that another
  developer had created was also granted `owner` on that module node.

  **Who is impacted:** stores where several developers upload versions of the same module.
  Released versions (5.1.1 and earlier) are not affected: the elevated owner grant that carried
  the flaw was introduced after 5.1.1 by the SEC-366 fix.

  **What changes:** the developer who first uploads a module still owns the module node and every
  version they upload. Another developer who uploads a version of that module now owns **only the
  version node** they created, not the module node. Store developers can still edit the
  information of each other's modules, as before: that comes from the `store-developer` role,
  not from the `owner` grant.

  **What to do:** nothing. No configuration or migration step is required.
