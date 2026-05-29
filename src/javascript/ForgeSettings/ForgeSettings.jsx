import React, {useEffect, useState} from 'react';
import {useMutation, useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Button, Field, Input} from '@jahia/moonstone';
import styles from './ForgeSettings.scss';
import {GET_FORGE_SETTINGS, UPDATE_FORGE_SETTINGS} from './ForgeSettings.gql';

export function ForgeSettings({siteKey}) {
    const {t} = useTranslation('privateappstore');

    const [url, setUrl] = useState('');
    const [id, setId] = useState('');
    const [user, setUser] = useState('');
    const [password, setPassword] = useState('');
    const [passwordSet, setPasswordSet] = useState(false);
    const [saveStatus, setSaveStatus] = useState(null);

    useEffect(() => {
        const prev = document.title;
        document.title = `${t('label.title')} — ${siteKey} — Jahia`;
        return () => {
            document.title = prev;
        };
    }, [siteKey, t]);

    const {data, loading, error} = useQuery(GET_FORGE_SETTINGS, {
        variables: {siteKey},
        fetchPolicy: 'network-only'
    });

    const [updateForgeSettings, {loading: saving}] = useMutation(UPDATE_FORGE_SETTINGS, {
        refetchQueries: [{query: GET_FORGE_SETTINGS, variables: {siteKey}}]
    });

    const syncFromSettings = s => {
        setUrl(s.url || '');
        setId(s.id || '');
        setUser(s.user || '');
        setPassword('');
        setPasswordSet(Boolean(s.passwordSet));
    };

    useEffect(() => {
        if (data && data.forgeSettings) {
            syncFromSettings(data.forgeSettings);
        }
    }, [data]);

    const handleSubmit = async () => {
        setSaveStatus('saving');
        try {
            const result = await updateForgeSettings({
                variables: {
                    siteKey,
                    url: url || null,
                    id: id || null,
                    user: user || null,
                    // Blank password = keep existing. Mutation interprets blank
                    // as "leave alone" to match the legacy flow's behavior.
                    password: password || null
                }
            });
            setSaveStatus(result.data && result.data.updateForgeSettings ? 'success' : 'error');
            setPassword('');
        } catch (err) {
            console.error('Failed to update forge settings:', err);
            setSaveStatus('error');
        }
    };

    const handleCancel = () => {
        if (data && data.forgeSettings) {
            syncFromSettings(data.forgeSettings);
        }

        setSaveStatus('cancel');
    };

    if (loading) {
        return <div className={styles.forge_loading}>{t('label.loading')}</div>;
    }

    if (error) {
        return (
            <div className={styles.forge_error} role="alert">
                {t('errors.load.failed')}: {error.message}
            </div>
        );
    }

    return (
        <div>
            <div className={styles.forge_page_header}>
                <h2>{t('label.title')} — {siteKey}</h2>
            </div>
            <div className={styles.forge_container}>
                {saveStatus === 'success' && (
                    <div className={`${styles.forge_alert} ${styles.success}`}>
                        {t('success.update')}
                    </div>
                )}
                {saveStatus === 'error' && (
                    <div className={`${styles.forge_alert} ${styles.error}`} role="alert">
                        {t('errors.update.failed')}
                    </div>
                )}

                <div className={styles.forge_form}>
                    <Field label={t('label.url')} id="forge-url">
                        <Input
                            id="forge-url"
                            value={url}
                            placeholder="https://store.jahia.com"
                            onChange={e => setUrl(e.target.value)}
                        />
                    </Field>

                    <Field label={t('label.id')} id="forge-id">
                        <Input
                            id="forge-id"
                            value={id}
                            onChange={e => setId(e.target.value)}
                        />
                    </Field>

                    <Field label={t('label.user')} id="forge-user">
                        <Input
                            id="forge-user"
                            value={user}
                            onChange={e => setUser(e.target.value)}
                        />
                    </Field>

                    <Field label={t('label.password')} id="forge-password">
                        <Input
                            id="forge-password"
                            type="password"
                            value={password}
                            placeholder={passwordSet ? t('label.password.unchanged') : ''}
                            onChange={e => setPassword(e.target.value)}
                        />
                        <div className={styles.forge_password_hint}>
                            {passwordSet ? t('label.password.replaceHint') : t('label.password.newHint')}
                        </div>
                    </Field>

                    <div className={styles.forge_actions}>
                        <Button
                            type="button"
                            label={saving ? t('label.saving') : t('label.save')}
                            variant="primary"
                            isDisabled={saving}
                            onClick={handleSubmit}
                        />
                        <Button
                            type="button"
                            label={t('label.cancel')}
                            variant="secondary"
                            isDisabled={saving}
                            onClick={handleCancel}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
}

export default ForgeSettings;
