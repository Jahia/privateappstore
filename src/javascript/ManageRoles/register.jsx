import {registry} from '@jahia/ui-extender';
import React from 'react';
import ManageRoles from './ManageRoles';

function ManageRolesRoute(props) {
    const siteKey =
        (props.match && props.match.params && props.match.params.siteKey) ||
        window.contextJsParameters.siteKey;

    if (!siteKey) {
        return <div>No site selected.</div>;
    }

    return <ManageRoles siteKey={siteKey}/>;
}

export default () => {
    registry.add('adminRoute', 'storeRoles', {
        icon: window.jahia.moonstone.toIconComponent('Person'),
        targets: ['administration-sites-forgeAdministration:30'],
        requiredPermission: 'siteAdminForgeSettings',
        label: 'jahia-store:roles.menu_entry',
        isSelectable: true,
        render: () => React.createElement(ManageRolesRoute)
    });
};
