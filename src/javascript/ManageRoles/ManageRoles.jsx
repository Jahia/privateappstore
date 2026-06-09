import React, {useEffect, useState} from 'react';
import {useLazyQuery, useMutation, useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Button, Field, Input} from '@jahia/moonstone';
import styles from './ManageRoles.scss';
import {
    GET_MANAGE_ROLES_SETTINGS,
    GRANT_SITE_ROLE,
    REVOKE_SITE_ROLE,
    SEARCH_FORGE_PRINCIPALS
} from './ManageRoles.gql';

export function ManageRoles({siteKey}) {
    const {t} = useTranslation('jahia-store');

    const [openRole, setOpenRole] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [searchType, setSearchType] = useState('USER');
    const [status, setStatus] = useState(null);
    const [statusMessage, setStatusMessage] = useState('');

    useEffect(() => {
        const prev = document.title;
        document.title = `${t('roles.title')} — ${siteKey} — Jahia`;
        return () => {
            document.title = prev;
        };
    }, [siteKey, t]);

    const {data, loading, error} = useQuery(GET_MANAGE_ROLES_SETTINGS, {
        variables: {siteKey},
        fetchPolicy: 'network-only'
    });

    const refetchQueries = [{query: GET_MANAGE_ROLES_SETTINGS, variables: {siteKey}}];
    const [grantRole] = useMutation(GRANT_SITE_ROLE, {refetchQueries});
    const [revokeRole] = useMutation(REVOKE_SITE_ROLE, {refetchQueries});
    const [searchPrincipals, {data: searchData, loading: searching}] = useLazyQuery(SEARCH_FORGE_PRINCIPALS, {
        fetchPolicy: 'network-only'
    });

    const reportSuccess = key => {
        setStatus('success');
        setStatusMessage(t(key));
    };

    const reportError = err => {
        console.error(err);
        setStatus('error');
        setStatusMessage(err && err.message ? err.message : t('errors.update.failed'));
    };

    const handleSearch = () => {
        if (!searchTerm.trim()) {
            return;
        }

        searchPrincipals({variables: {siteKey, searchTerm: searchTerm.trim(), type: searchType}});
    };

    const handleGrant = async principal => {
        try {
            await grantRole({
                variables: {
                    siteKey,
                    role: openRole,
                    principalName: principal.name,
                    principalType: principal.type
                }
            });
            reportSuccess('roles.success.granted');
            setOpenRole(null);
            setSearchTerm('');
        } catch (err) {
            reportError(err);
        }
    };

    const handleRevoke = async (role, member) => {
        const confirmed = window.confirm(t('roles.revoke.confirm', {
            principal: member.displayName || member.name,
            role
        }));
        if (!confirmed) {
            return;
        }

        try {
            await revokeRole({
                variables: {
                    siteKey,
                    role,
                    principalName: member.name,
                    principalType: member.type
                }
            });
            reportSuccess('roles.success.revoked');
        } catch (err) {
            reportError(err);
        }
    };

    if (loading) {
        return <div className={styles.roles_loading}>{t('label.loading')}</div>;
    }

    if (error) {
        return (
            <div className={styles.roles_error} role="alert">
                {t('errors.load.failed')}: {error.message}
            </div>
        );
    }

    const settings = data && data.manageRolesSettings;
    if (!settings) {
        return <div className={styles.roles_error} role="alert">{t('errors.load.failed')}</div>;
    }

    return (
        <div>
            <div className={styles.roles_page_header}>
                <h2>{t('roles.title')} — {siteKey}</h2>
            </div>
            <div className={styles.roles_container}>
                {status === 'success' && (
                    <div className={`${styles.roles_alert} ${styles.success}`} role="status">{statusMessage}</div>
                )}
                {status === 'error' && (
                    <div className={`${styles.roles_alert} ${styles.error}`} role="alert">{statusMessage}</div>
                )}

                {settings.roles.map(roleEntry => (
                    <section key={roleEntry.role} className={styles.roles_role}>
                        <h3 className={styles.roles_role_title}>
                            {t(`roles.names.${roleEntry.role}`, {defaultValue: roleEntry.role})}
                        </h3>

                        {roleEntry.members.length === 0 ? (
                            <div className={styles.roles_empty}>{t('roles.empty')}</div>
                        ) : (
                            <ul className={styles.roles_members}>
                                {roleEntry.members.map(member => (
                                    <li key={`${member.type}:${member.name}`} className={styles.roles_member}>
                                        <span>
                                            {member.displayName || member.name}
                                            <span className={styles.roles_member_type}>{member.type}</span>
                                        </span>
                                        <Button
                                            type="button"
                                            label={t('label.revoke')}
                                            variant="danger"
                                            size="small"
                                            onClick={() => handleRevoke(roleEntry.role, member)}
                                        />
                                    </li>
                                ))}
                            </ul>
                        )}

                        {openRole === roleEntry.role ? (
                            <div>
                                <div className={styles.roles_add_row}>
                                    <div className={styles.roles_type_toggle}>
                                        <Button
                                            type="button"
                                            label={t('roles.search.users')}
                                            variant={searchType === 'USER' ? 'primary' : 'secondary'}
                                            size="small"
                                            onClick={() => setSearchType('USER')}
                                        />
                                        <Button
                                            type="button"
                                            label={t('roles.search.groups')}
                                            variant={searchType === 'GROUP' ? 'primary' : 'secondary'}
                                            size="small"
                                            onClick={() => setSearchType('GROUP')}
                                        />
                                    </div>
                                    <div className={styles.roles_add_search}>
                                        <Field label={t('roles.search.label')} id={`search-${roleEntry.role}`}>
                                            <Input
                                                id={`search-${roleEntry.role}`}
                                                placeholder={t('roles.search.placeholder')}
                                                value={searchTerm}
                                                onChange={e => setSearchTerm(e.target.value)}
                                                onKeyDown={e => e.key === 'Enter' && handleSearch()}
                                            />
                                        </Field>
                                    </div>
                                    <Button
                                        type="button"
                                        label={searching ? t('label.loading') : t('roles.search.action')}
                                        variant="secondary"
                                        size="small"
                                        isDisabled={searching || !searchTerm.trim()}
                                        onClick={handleSearch}
                                    />
                                    <Button
                                        type="button"
                                        label={t('label.cancel')}
                                        variant="ghost"
                                        size="small"
                                        onClick={() => {
                                            setOpenRole(null);
                                            setSearchTerm('');
                                        }}
                                    />
                                </div>
                                {searchData && searchData.searchForgePrincipals && searchData.searchForgePrincipals.length > 0 ? (
                                    <ul className={styles.roles_search_results}>
                                        {searchData.searchForgePrincipals.map(p => (
                                            <li
                                                key={`${p.type}:${p.name}`}
                                                className={styles.roles_search_result}
                                                role="button"
                                                tabIndex={0}
                                                onClick={() => handleGrant(p)}
                                                onKeyDown={e => {
                                                    if (e.key === 'Enter' || e.key === ' ') {
                                                        e.preventDefault();
                                                        handleGrant(p);
                                                    }
                                                }}
                                            >
                                                {p.displayName || p.name} <span className={styles.roles_member_type}>{p.type}</span>
                                            </li>
                                        ))}
                                    </ul>
                                ) : (searchData && (
                                    <div className={styles.roles_empty}>{t('roles.search.noResults')}</div>
                                ))}
                            </div>
                        ) : (
                            <Button
                                type="button"
                                label={t('roles.addMember')}
                                variant="secondary"
                                size="small"
                                onClick={() => {
                                    setOpenRole(roleEntry.role);
                                    setSearchTerm('');
                                }}
                            />
                        )}
                    </section>
                ))}
            </div>
        </div>
    );
}

export default ManageRoles;
