# Security Policy

## Reporting a Vulnerability

Security information can be found in our [security.txt file](https://academy.jahia.com/.well-known/security.txt).

## Operator notes

### GHSA-f882-3xwv-3439 — `store-developer` could rewrite the site's live ACL

Affects `jahia-store` **5.1.1 and earlier**. The `store-developer` role — granted to everyone
allowed to upload a module — shipped `jcr:modifyAccessControl_live` at site scope. A store
developer could rewrite the site's access-control list in the **live** workspace: promote
themselves to `store-administrator` (which confers `jahiaForgeModerateModule`, the permission
gating whether an uploaded module is published to the store), promote an unrelated third party, or
remove the site administrators' own entry.

**Upgrading is sufficient in normal circumstances.** Module role data is imported only once per
module version, so a new version does not by itself rewrite a role node already present in the
JCR. The module therefore repairs the role itself on start
(`org.jahia.modules.forge.migration.StoreDeveloperRoleMigration`). A successful repair logs, once:

```
SEC-366: removed jcr:modifyAccessControl_live from role store-developer in workspace default (1 reference(s))
```

A role that is already clean logs nothing.

**If the repair fails** it logs at `ERROR` and the installation is still exposed. Remove the
permission by hand from `/roles/store-developer` — through *Administration → Roles and
permissions*, or by clearing the `Modify access control` privilege for the live workspace on that
role.

### Auditing whether the ACL was tampered with

The tampering this advisory describes is **invisible in the editing UI**, because only the live
workspace was written and nothing entered a publication queue. An administrator inspecting roles
and permissions the usual way sees an intact, correct list.

The signature to look for is **a live access-control entry with no default-workspace
counterpart** — most tellingly, a grant of `store-administrator` or `site-administrator` on the
site node that exists in `live` but not in `default`, or a *missing* `g:site-administrators` entry
in `live` that is still present in `default`. Compare the two workspaces directly rather than
trusting either alone:

```graphql
{
  edit: jcr(workspace: EDIT) { nodeByPath(path: "/sites/<siteKey>/j:acl") {
    children { nodes { name } } } }
  live: jcr(workspace: LIVE) { nodeByPath(path: "/sites/<siteKey>/j:acl") {
    children { nodes { name } } } }
}
```

Any ACE present in `live` but absent from `edit` was written directly to the live workspace and
never reviewed. Note that a legitimate live-only entry is possible in principle, so treat a
difference as something to explain rather than as proof on its own.
