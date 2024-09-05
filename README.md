# Villager Announcer
 
This minecraft plugin will announce villager deaths, infections and more to all players on the server.
Just place the jar file into your plugins directory and you're good to good.

You can change the options in config.yml:
```yaml
enabled: true
play-sound: true
sound-name: 'ENTITY_VILLAGER_DEATH'

# use 0 for unlimited radius
max-broadcast-radius: 0
# if you're traded with a villager previously then you will get notified
# note this is only compatible on Paper servers
only-broadcast-if-traded-with: false
# optionally limit broadcasts to specific worlds
broadcast-worlds: ['*']
# only broadcasts if they were formally normal villagers
broadcast-zombie-villager-deaths: true
# villagerannouncer.receive-broadcasts
players-require-premissions: false
# if DiscordSRV is installed then send any messages to the main text channel
discordsrv-send-message-to-main-channel: true
# this is only applicable if you have world filtering
# or are using player permisisons
log-messages-to-console: true

messages:
  # all four of these settings will populate the %villager% variable:
  villager: 'villager'
  baby-villager: 'baby villager'
  zombie-villager: 'zombie villager'
  baby-zombie-villager: 'baby zombie villager'

  # populates %location% variable:
  location: '&r( &6XYZ: %x% %y% %z%, &r&ein &r&a%world-name%&r)'
  villager-message-with-profession: '&eA %villager% has died! Profession: %villager-profession%, level: %villager-level% %location%'
  villager-message: '&eA %villager% has died! %location%'
  villager-infection-with-profession: '&eA %villager% has been infected! Profession: %villager-profession%, level: %villager-level% %location%'
  villager-infection: '&eA %villager% has been infected! %location%'
  death-by-entity: '&eA %villager% was killed by %entity% %location%'
  death-by-misc: '&eA %villager% died by %death-cause% %location%'

# Available variables:
# %villager%
# %player%
# %entity%
# %entity-type%
# %death-cause%
# %world-name%
# %world-type%
# %villager-profession%
# %villager-level%
# %villager-experience%
# %villager-type%

file-version: 3
```

Questions or feature suggestions? Join the [discord](https://discord.gg/arcaneplugins-752310043214479462) server.
