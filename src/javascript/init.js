import {registry} from '@jahia/ui-extender';
import registerForgeSettings from './ForgeSettings/register';
import registerCategorySettings from './CategorySettings/register';
import registerManageRoles from './ManageRoles/register';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'jahia-store', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces('jahia-store', () => {
                console.debug('%c jahia-store: i18n namespace loaded', 'color: #463CBA');
            });
            // Non-selectable parent that groups Settings / Categories / Roles under a
            // single "Store administration" entry in the site administration menu.
            // The three leaves below target this route (forgeAdministration:NN).
            registry.add('adminRoute', 'forgeAdministration', {
                targets: ['administration-sites:998'],
                icon: window.jahia.moonstone.toIconComponent('Apps'),
                label: 'jahia-store:administration.menu_entry',
                requiredPermission: 'siteAdminForgeSettings',
                isSelectable: false
            });
            registerForgeSettings();
            registerCategorySettings();
            registerManageRoles();
            console.debug('%c jahia-store: activation completed', 'color: #463CBA');
        }
    });
}
