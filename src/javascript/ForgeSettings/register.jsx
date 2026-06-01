import {registry} from '@jahia/ui-extender';
import React from 'react';
import ForgeSettings from './ForgeSettings';

function ForgeSettingsRoute(props) {
    const siteKey =
        (props.match && props.match.params && props.match.params.siteKey) ||
        window.contextJsParameters.siteKey;

    if (!siteKey) {
        return <div>No site selected.</div>;
    }

    return <ForgeSettings siteKey={siteKey}/>;
}

export default () => {
    registry.add('adminRoute', 'forgeSettings', {
        icon: window.jahia.moonstone.toIconComponent('Setting'),
        targets: ['administration-sites-forgeAdministration:10'],
        requiredPermission: 'siteAdminForgeSettings',
        label: 'privateappstore:label.menu_entry',
        isSelectable: true,
        render: () => React.createElement(ForgeSettingsRoute)
    });
};
