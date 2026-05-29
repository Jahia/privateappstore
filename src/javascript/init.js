import {registry} from '@jahia/ui-extender';
import registerForgeSettings from './ForgeSettings/register';
import registerCategorySettings from './CategorySettings/register';
import registerManageRoles from './ManageRoles/register';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'privateappstore', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces('privateappstore', () => {
                console.debug('%c privateappstore: i18n namespace loaded', 'color: #463CBA');
            });
            registerForgeSettings();
            registerCategorySettings();
            registerManageRoles();
            console.debug('%c privateappstore: activation completed', 'color: #463CBA');
        }
    });
}
