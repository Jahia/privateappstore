import {registry} from '@jahia/ui-extender';
import React from 'react';
import CategorySettings from './CategorySettings';

function CategorySettingsRoute(props) {
    const siteKey =
        (props.match && props.match.params && props.match.params.siteKey) ||
        window.contextJsParameters.siteKey;

    if (!siteKey) {
        return <div>No site selected.</div>;
    }

    return <CategorySettings siteKey={siteKey}/>;
}

export default () => {
    registry.add('adminRoute', 'categorySettings', {
        icon: window.jahia.moonstone.toIconComponent('Tag'),
        targets: ['administration-sites:1000'],
        requiredPermission: 'siteAdminForgeSettings',
        label: 'privateappstore:categories.menu_entry',
        isSelectable: true,
        render: () => React.createElement(CategorySettingsRoute)
    });
};
